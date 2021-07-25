package com.example.demo.controller;

import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.param.IntParam;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.StructType;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.col;

public class WuzzufServiceDAO {
    // start spark session
    public DataFrameReader getFrameReader(){
        final SparkSession session = SparkSession.builder().appName("CSV analysis").master("local[*]").getOrCreate();
        return session.read();
    }

    // read the csv into RDD
    public Dataset<Row> getDataset() {
        Dataset<Row> data = getFrameReader().option("header", "true").csv("src\\main\\resources\\wuzzufjobs.csv").na().drop();
        data = data.dropDuplicates().filter((FilterFunction<Row>) row -> !row.get(5).equals("null Yrs of Exp")).cache();
        return data;
    }

    Dataset<Row> data = getDataset();

    // get the first 10 rows of data
    public String ShowData(){
        List<Row> first_10_records = data.limit(10).collectAsList();
        return DisplayHtml.displayrows(data.columns(), first_10_records);
    }

    // get the structure of the data
    public String structure(){
        StructType d = data.schema();
        return d.prettyJson();

    }


    // get the summary statistics of the data
    public String summary() {
        Dataset<Row> d = data.summary();
        List<Row> summary = d.collectAsList();
        return DisplayHtml.displayrows(d.columns(), summary);
    }

    // get the top 10 companies that are hiring
    public String JobsByCompany(){
        Dataset<Row> groupeddatabycompany = data.groupBy("Company").count().orderBy(col("count").desc()).limit(10);
         List<Row> top_Companies = groupeddatabycompany.collectAsList();
        return DisplayHtml.displayrows(groupeddatabycompany.columns(), top_Companies);
    }

    // create a pie chart of top 10 companies that are hiring
    public String getPieChartforCompany() throws IOException {
        Dataset<Row> groupeddatabycompany = data.groupBy("Company").count().orderBy(col("count").desc()).limit(10);;
        List<String> companies = groupeddatabycompany.select("Company").as(Encoders.STRING()).collectAsList();
        List<String> counts = groupeddatabycompany.select("count").as(Encoders.STRING()).collectAsList();

        // Create Chart
        PieChart chart =
                new PieChartBuilder().width(800).height(600).title("Pie chart for companies").build();

        // Customize Chart
        chart.getStyler().setCircular(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(Styler.LegendLayout.Horizontal);

        for (int i=0; i < companies.size() ; i++) {
            chart.addSeries(companies.get(i), Integer.parseInt(counts.get(i)));
        }

        String path = "src\\main\\resources\\puplic\\Sample_pieChart.png";
        BitmapEncoder.saveBitmap(chart,path, BitmapEncoder.BitmapFormat.PNG);
        return DisplayHtml.viewchart(path);
    }

    // get the top 30 jobs needed
    public String JobsByTitles(){
        Dataset<Row> groupeddatabytitle = data.groupBy("Title").count().orderBy(col("count").desc()).limit(30);;
        List<Row> top_titles = groupeddatabytitle.collectAsList();
        return DisplayHtml.displayrows(groupeddatabytitle.columns(), top_titles);
    }

    // create a bar chart of the top 10 jobs needed
    public String TitlesBarChart() throws IOException {
        Dataset<Row> groupeddatabytitles = data.groupBy("Title").count().orderBy(col("count").desc()).limit(10);
        List<String> titles = groupeddatabytitles .select("Title").as(Encoders.STRING()).collectAsList();
        List<Long> counts = groupeddatabytitles .select("count").as(Encoders.LONG()).collectAsList();

        CategoryChart CH = CategoryBarChart.barChart("Titles Vs Counts", "Titles", titles, counts);
        String path = "src\\main\\resources\\puplic\\Titles_barChart.png";
        BitmapEncoder.saveBitmap(CH,path, BitmapEncoder.BitmapFormat.PNG);
        return DisplayHtml.viewchart(path);
    }

    // get the top 30 locations
    public String JobsByAreas(){
        Dataset<Row> groupeddatabyareas = data.groupBy("Location").count().orderBy(col("count").desc()).limit(30);;
        List<Row> top_titles = groupeddatabyareas.collectAsList();
        return DisplayHtml.displayrows(groupeddatabyareas.columns(), top_titles);
    }

    // create a bar chart of the top 30 locations
    public String areasBarChart() throws IOException {
        Dataset<Row> groupeddatabylocation = data.groupBy("Location").count().orderBy(col("count").desc()).limit(10);
        List<String> location = groupeddatabylocation .select("Location").as(Encoders.STRING()).collectAsList();
        List<Long> counts = groupeddatabylocation .select("count").as(Encoders.LONG()).collectAsList();

        CategoryChart CH = CategoryBarChart.barChart("Locations Vs Counts", "Locations", location, counts);
        String path = "src\\main\\resources\\puplic\\Location_barChart.png";
        BitmapEncoder.saveBitmap(CH,path, BitmapEncoder.BitmapFormat.PNG);
        return DisplayHtml.viewchart(path);
    }

    // get the top 10 skills needed for hiring
    public ResponseEntity<Object> skill() {
        List<String> allskills = data.select("Skills").as(Encoders.STRING()).collectAsList();
        List<String> skills = new ArrayList<>();
        for (String ls : allskills) {
            String[] x = ls.split(",");
            for (String s : x) {
                skills.add(s);
            }
        }

        Map<String, Long> skill_counts =
                skills.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        List<String> sk = new ArrayList<>();
        List<Long> countsk = new ArrayList<>();
        for (Map.Entry<String, Long> m : skill_counts.entrySet()) {
            sk.add(m.getKey());
            countsk.add(m.getValue());
        }

        return new ResponseEntity<>(skill_counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10), HttpStatus.OK);

    }

}

