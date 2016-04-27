package es.qopuir.basicfitbot.internal;

import java.io.IOException;
import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import es.qopuir.basicfitbot.Command;
import es.qopuir.basicfitbot.CommandHandler;
import es.qopuir.basicfitbot.Methods;
import es.qopuir.basicfitbot.model.Chat;
import es.qopuir.basicfitbot.repo.ChatRepository;
import es.qopuir.telegrambot.model.Update;

@Component
public class CommandHandlerImpl implements CommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommandHandlerImpl.class);

    @Autowired
    private Methods methods;

    @Autowired
    // TODO (dcastro): crear un servicio transaccional para acceder al repositorio
    private ChatRepository chatRepository;

    @Override
    public void handleCommand(Update update, Command command) throws MalformedURLException, IOException {
        switch (command.getCommand()) {
            case HELP:
                sendIntroductionMessage(update);
                break;
            case START:
                if (!StringUtils.isEmpty(command.getArgs())) {
                    // TODO (dcastro): validate received buildingId
                    Chat chat = new Chat();
                    chat.setChatId(update.getMessage().getChat().getId());
                    chat.setBuildingId(command.getArgs().trim());

                    chatRepository.save(chat);
                }

                sendIntroductionMessage(update);

                break;
            case UNKNOWN:
                sendInformationMessage(update);
                break;
            default:
                handleIdealistaCommand(update, command);
                break;
        }
    }

    private void sendIntroductionMessage(Update update) {
        methods.sendMessage(update.getMessage().getChat().getId(), "You like the weather charts from the dmi.dk site?" + System.lineSeparator()
                + "This bot can show you the weather forecast graphs for your desired city." + System.lineSeparator()
                + "The following commands can be used:" + System.lineSeparator() + System.lineSeparator()
                + "/now cityname - showing the two day weather" + System.lineSeparator() + "/week cityname - showing furhter weather of the week"
                + System.lineSeparator() + System.lineSeparator() + "This bot project can be found at https://github.com/SimonScholz/telegram-bot");
    }

    private void sendInformationMessage(Update update) {
        methods.sendMessage(update.getMessage().getChat().getId(),
                "Command received (" + update.getMessage().getText() + ") is not well formatted." + System.lineSeparator()
                        + "We are sorry to not be able to process it." + System.lineSeparator() + "The following commands can be used:"
                        + System.lineSeparator() + System.lineSeparator() + "/now cityname - showing the two day weather" + System.lineSeparator()
                        + "/week cityname - showing furhter weather of the week" + System.lineSeparator() + System.lineSeparator()
                        + "This bot project can be found at https://github.com/SimonScholz/telegram-bot");
    }

    private void handleIdealistaCommand(Update update, Command command) throws MalformedURLException, IOException {
        LOG.debug("Command received {} with arguments {}", command.getCommand(), command.getArgs());
    }
}