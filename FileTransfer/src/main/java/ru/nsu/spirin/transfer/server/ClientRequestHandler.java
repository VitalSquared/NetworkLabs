package ru.nsu.spirin.transfer.server;

import lombok.RequiredArgsConstructor;
import ru.nsu.spirin.transfer.protocol.ResponseCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RequiredArgsConstructor
public class ClientRequestHandler implements Runnable {
    private static final int BUF_SIZE = 1024;
    private static final long SPEED_TEST_INTERVAL_MILLIS = 3000;
    private static final String FILE_STORAGE_FOLDER = "uploads";

    private final Socket socket;

    @Override
    public void run() {
        try (DataOutputStream clientWriter = new DataOutputStream(socket.getOutputStream());
                DataInputStream clientReader = new DataInputStream(socket.getInputStream());
                this.socket) {

            String fileName = getFileName(clientReader, clientWriter);
            if (fileName == null) {
                return;
            }
            sendFeedback(clientWriter, ResponseCode.SUCCESS_FILENAME_TRANSFER);

            long fileSize = clientReader.readLong();
            Path path = createFile(fileName);
            long receivedSize = getFileContent(path, fileSize, clientReader);
            sendFeedback(clientWriter, receivedSize == fileSize ? ResponseCode.SUCCESS_FILENAME_TRANSFER : ResponseCode.FAILURE_FILE_TRANSFER);
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private String getFileName(DataInputStream clientReader, DataOutputStream clientWriter) throws IOException {
        int fileNameSize = clientReader.readInt();
        String fileName = clientReader.readUTF();
        if (fileNameSize != fileName.length()) {
            System.out.println("File name length does not match with original!");
            sendFeedback(clientWriter, ResponseCode.FAILURE_FILENAME_TRANSFER);
            return null;
        }
        return fileName;
    }

    private long getFileContent(Path path, long fileSize, DataInputStream clientReader) {
        byte[] buffer = new byte[BUF_SIZE];
        long curBytesRead = 0;
        long prevBytesRead = 0;
        long startTime = System.currentTimeMillis();
        long curTime = startTime;
        long lastTime = startTime;
        boolean activeLessThanRequired = true;

        try (OutputStream fileWriter = Files.newOutputStream(path)) {
            while (curBytesRead < fileSize) {
                int bytesRead;
                if ((bytesRead = clientReader.read(buffer, 0, BUF_SIZE)) >= 0) {
                    fileWriter.write(buffer, 0, bytesRead);
                }
                curBytesRead += bytesRead;

                curTime = System.currentTimeMillis();
                if (SPEED_TEST_INTERVAL_MILLIS < (curTime - lastTime)) {
                    long curSpeed = (curBytesRead - prevBytesRead) * 1000 / (curTime - lastTime);
                    long avgSpeed = curBytesRead * 1000 / (curTime - startTime);
                    System.out.println("File transfer {" + path.getFileName() + "} has current speed = " + curSpeed + " bytes/s, avg speed = " + avgSpeed + " bytes/s");
                    lastTime = curTime;
                    activeLessThanRequired = false;
                    prevBytesRead = curBytesRead;
                }
            }
            if (activeLessThanRequired) {
                long speed = curBytesRead * 1000 / (System.currentTimeMillis() - lastTime);
                System.out.println("File transfer {" + path.getFileName() + "} had speed = " + speed + " bytes/s");
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }

        return curBytesRead;
    }

    private Path createFile(String fileName) throws IOException {
        Path dirPath = Paths.get(FILE_STORAGE_FOLDER);
        if (!Files.exists(dirPath)) {
            Files.createDirectory(dirPath);
        }
        Path path = Paths.get(dirPath + System.getProperty("file.separator") + fileName);
        if (Files.exists(path)) {
            path = Paths.get(dirPath + System.getProperty("file.separator") + generateRandomFileName(fileName));
        }
        Files.createFile(path);
        return path;
    }

    private String generateRandomFileName(String fileName) {
        UUID uuid = UUID.randomUUID();
        return fileName + "_" + uuid;
    }

    private void sendFeedback(DataOutputStream writer, ResponseCode responseCode) throws IOException {
        writer.writeInt(responseCode.getCode());
        writer.flush();
    }
}
