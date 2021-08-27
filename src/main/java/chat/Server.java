package chat;



import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Введите порт чат сервера:");
        int port = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port);) {
            System.out.println("Сервер чата запущен");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Handler handler = new Handler(clientSocket);
                handler.start();  
            }
        } catch (IOException e) {
            System.out.println("Произошла ошибка подключения на сервере:\n" + e.getMessage());
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение с " + socket.getRemoteSocketAddress());
            String userName = null;

            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);

                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);

                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом " +
                                    socket.getRemoteSocketAddress());
            }

            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }

            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message userMessage = connection.receive();

                if (userMessage.getType() == MessageType.TEXT) {
                    String message = userName + ": " + userMessage.getData();
                    Message serverMessage = new Message(MessageType.TEXT, message);
                    sendBroadcastMessage(serverMessage);
                } else {
                    ConsoleHelper.writeMessage("Некорректный формат сообщения от " + socket.getRemoteSocketAddress());
                }
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String mapUser : connectionMap.keySet()) {
                if (!mapUser.equals(userName)) {
                    Message message = new Message(MessageType.USER_ADDED, mapUser);
                    connection.send(message);
                }
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            ConsoleHelper.writeMessage("Запрошено имя пользователя от " + socket.getRemoteSocketAddress());
            Message nameRequest = new Message(MessageType.NAME_REQUEST);
            connection.send(nameRequest);

            Message response = connection.receive();

            while (response.getType() != MessageType.USER_NAME || response.getData() == null ||
                    response.getData().isEmpty() || connectionMap.containsKey(response.getData())) {
                ConsoleHelper.writeMessage("Полученный ответ от " +
                        socket.getRemoteSocketAddress() + "не соответствует протоколу");
                ConsoleHelper.writeMessage("Запрошено имя пользователя от " + socket.getRemoteSocketAddress());
                connection.send(nameRequest);
                response = connection.receive();
            }

            String userName = response.getData();
            connectionMap.put(userName, connection);

            connection.send(new Message(MessageType.NAME_ACCEPTED));

            return userName;
        }
    }

    public static void sendBroadcastMessage(Message message) {
        connectionMap.forEach((client, connection) -> {
            try {
                connection.send(message);
            } catch (IOException e) {
                System.out.println("Ошибка отправки сообщения пользователю " + client);
            }
        });
    }
}
