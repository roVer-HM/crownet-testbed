package edu.hm.crownet.testbed.analytics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BeaconLogger {

  private FileWriter writer;          // <- nicht final, damit wir neu öffnen können
  private final String filePath;

  public BeaconLogger(String filePath) throws IOException {
    this.filePath = filePath;

    var path = Path.of(filePath);
    var parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    boolean empty = !Files.exists(path) || Files.size(path) == 0;
    this.writer = new FileWriter(filePath, true);
    if (empty) {
      writer.write("timestamp,sourceId,sequenceNo,sizeBytes\n");
      writer.flush();
    }
  }

  public synchronized void log(BeaconLog log) throws IOException {
    writer.write(String.format("%s,%d,%d,%d%n",
            log.timestamp().getTime(),
            log.sourceId(),
            log.sequenceNo(),
            log.sizeBytes()));
    writer.flush();
  }

  public synchronized void clear() throws IOException {
    writer.close();
    try (FileWriter w = new FileWriter(filePath, false)) {
      w.write("timestamp,sourceId,sequenceNo,sizeBytes\n");
      w.flush();
    }
    this.writer = new FileWriter(filePath, true);
  }

  public synchronized void close() throws IOException {
    writer.close();
  }

  public synchronized List<BeaconLog> readAll() throws IOException {
    List<BeaconLog> logs = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      boolean header = true;
      while ((line = reader.readLine()) != null) {
        if (header) {
          header = false;
          continue;
        }
        String[] parts = line.split(",");
        if (parts.length != 4) continue;
        long ts = Long.parseLong(parts[0]);
        int sourceId = Integer.parseInt(parts[1]);
        int seq = Integer.parseInt(parts[2]);
        int size = Integer.parseInt(parts[3]);
        logs.add(new BeaconLog(new Timestamp(ts), sourceId, seq, size));
      }
    }
    return logs;
  }
}