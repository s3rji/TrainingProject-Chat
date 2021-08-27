package chat.client;

import chat.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class BotClient extends Client {

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random()*100);
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");

            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);

            String[] splittedMessage = message.split(":");
            if (splittedMessage.length != 2) return;

            Calendar calendar = new GregorianCalendar();
            switch (splittedMessage[1].trim()) {
                case "дата":
                    SimpleDateFormat dateFormat = new SimpleDateFormat("d.MM.yyyy");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                                                dateFormat.format(calendar.getTime()));
                    break;
                case "день":
                    SimpleDateFormat dayFormat = new SimpleDateFormat("d");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                                                dayFormat.format(calendar.getTime()));
                    break;
                case "месяц":
                    SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                            monthFormat.format(calendar.getTime()));
                    break;
                case "год":
                    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                            yearFormat.format(calendar.getTime()));
                    break;
                case "время":
                    SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm:ss");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                            timeFormat.format(calendar.getTime()));
                    break;
                case "час":
                    SimpleDateFormat hoursFormat = new SimpleDateFormat("H");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                            hoursFormat.format(calendar.getTime()));
                    break;
                case "минуты":
                    SimpleDateFormat minutesFormat = new SimpleDateFormat("m");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                            minutesFormat.format(calendar.getTime()));
                    break;
                case "секунды":
                    SimpleDateFormat secondsFormat = new SimpleDateFormat("s");
                    sendTextMessage("Информация для " + splittedMessage[0] + ": " +
                            secondsFormat.format(calendar.getTime()));
                    break;
            }
        }
    }
}
