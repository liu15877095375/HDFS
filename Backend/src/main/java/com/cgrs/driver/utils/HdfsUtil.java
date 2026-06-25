package com.cgrs.driver.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class HdfsUtil {
    @Value("${hdfs.uri}")
    private String hdfsUri;
    @Value("${hadoop.hdfs.user}")
    private String hdfsUser;

    public String readFile(String hdfsPath) throws Exception {
        Configuration conf = new Configuration();
        conf.set("dfs.client.use.datanode.hostname", "true");
        conf.set("dfs.datanode.use.datanode.hostname", "true");
        FileSystem fs = FileSystem.get(new URI(hdfsUri), conf, hdfsUser);
        Path path = new Path(hdfsPath);
        if (!fs.exists(path)) return null;
        try (FSDataInputStream is = fs.open(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } finally {
            fs.close();
        }
    }
}