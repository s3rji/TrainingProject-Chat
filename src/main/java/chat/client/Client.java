package chat.client;

import chat.Connection;
import chat.ConsoleHelper;
import chat.Message;
import chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Произошла ошибка подключения");
                return;
            }
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено.\nДля выхода наберите команду 'exit'.");
        }
        else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }

        while (clientConnected) {
            String message = ConsoleHelper.readString();
            if (message.equals("exit")) break;

            if (shouldSendTextFromConsole()) sendTextMessage(message);
        }
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        String serverAddress = ConsoleHelper.readString();

        return serverAddress;
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        int serverPort = ConsoleHelper.readInt();

        return serverPort;
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя:");
        String userName = ConsoleHelper.readString();

        return userName;
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Произошла ошибка при отправке сообщения");
            clientConnected = false;
        }
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    public class SocketThread extends Thread {
        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();

            try {
                Socket clientSocket = new Socket(serverAddress, serverPort);
                connection = new Connection(clientSocket);

                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник " + userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник " + userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            Message message;

            while (true) {
                message = connection.receive();
                if (message.getType() == null) throw new IOException("Unexpected MessageType");

                switch (message.getType()) {
                    case NAME_REQUEST:
                        String userName = getUserName();
                        Message response = new Message(MessageType.USER_NAME, userName);
                        connection.send(response);
                        break;
                    case NAME_ACCEPTED:
                        notifyConnectionStatusChanged(true);
                        return;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            Message message;

            while (true) {
                message = connection.receive();
                if (message.getType() == null) throw new IOException("Unexpected MessageType");

                switch (message.getType()) {
                    case TEXT:
                        processIncomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        informAboutAddingNewUser(message.getData());
                        break;
                    case USER_REMOVED:
                        informAboutDeletingNewUser(message.getData());
                        break;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

    }
}
