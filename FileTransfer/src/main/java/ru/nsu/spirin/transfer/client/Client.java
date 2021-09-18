package ru.nsu.spirin.transfer.client;

import lombok.RequiredArgsConstructor;
import ru.nsu.spirin.transfer.exceptions.UnknownResponseCodeException;
import ru.nsu.spirin.transfer.protocol.ResponseCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public final class Client {
    private static final int BUF_SIZE = 1024;

    private final Path path;
    private final InetAddress serverAddress;
    private final int serverPort;

    public void run() {
        try (Socket socket = new Socket(this.serverAddress, this.serverPort);
             InputStream fileReader =  Files.newInputStream(this.path);
             DataOutputStream socketWriter = new DataOutputStream(socket.getOutputStream());
             DataInputStream socketReader = new DataInputStream(socket.getInputStream())) {

            if (!sendFileName(socketReader, socketWriter)) {
                return;
            }
            sendFileSize(socketWriter);
            sendFileContent(fileReader, socketReader, socketWriter);
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
        catch (UnknownResponseCodeException exception) {
            System.out.println("Unknown code from server: " + exception.getLocalizedMessage());
        }
    }

    private boolean sendFileName(DataInputStream socketReader, DataOutputStream socketWriter) throws IOException, UnknownResponseCodeException {
        String fileName = this.path.getFileName().toString();
        socketWriter.writeInt(fileName.length());
        socketWriter.writeUTF(fileName);
        socketWriter.flush();

        ResponseCode nameTransfer = ResponseCode.getResponseByCode(socketReader.readInt());
        if (ResponseCode.FAILURE_FILENAME_TRANSFER == nameTransfer) {
            System.out.println("Failed to transfer file name!");
            return false;
        }
        return true;
    }

    private void sendFileSize(DataOutputStream socketWriter) throws IOException {
        long fileSize = Files.size(this.path);
        socketWriter.writeLong(fileSize);
        socketWriter.flush();
    }

    private void sendFileContent(InputStream fileReader, DataInputStream socketReader, DataOutputStream socketWriter) throws IOException, UnknownResponseCodeException {
        byte[] buffer = new byte[BUF_SIZE];
        int lineSize;

        while (0 < (lineSize = fileReader.read(buffer, 0, BUF_SIZE))) {
            socketWriter.write(buffer, 0, lineSize);
            socketWriter.flush();
        }

        ResponseCode contentTransfer = ResponseCode.getResponseByCode(socketReader.readInt());
        if (ResponseCode.FAILURE_FILE_TRANSFER == contentTransfer) {
            System.out.println("Failed to transfer file!");
        }
    }
}
