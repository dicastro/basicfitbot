package es.qopuir.basicfitbot.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import es.qopuir.basicfitbot.Command;
import es.qopuir.basicfitbot.CommandHandler;
import es.qopuir.basicfitbot.Methods;
import es.qopuir.basicfitbot.back.BasicFitRest;
import es.qopuir.basicfitbot.model.Chat;
import es.qopuir.basicfitbot.repo.ChatRepository;
import es.qopuir.telegrambot.model.Update;

@Component
public class CommandHandlerImpl implements CommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommandHandlerImpl.class);

    @Autowired
    private BasicFitRest basicFitRest;
    
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
        methods.sendMessage(update.getMessage().getChat().getId(),
                "No sabes que clases hay hoy en tu ginmasio BasicFit?" + System.lineSeparator() + "No te preocupes BasicFitBot te lo pone muy facil!"
                        + System.lineSeparator() + "Envia:" + System.lineSeparator() + System.lineSeparator()
                        + "/horario - para recuperar el horario del dia" + System.lineSeparator() + System.lineSeparator()
                        + "Este bot se encuentra en https://github.com/dicastro/basicfitbot");
    }

    private void sendInformationMessage(Update update) {
        methods.sendMessage(update.getMessage().getChat().getId(),
                "El comando recibido (" + update.getMessage().getText() + ") no esta bien formateado." + System.lineSeparator()
                        + "Lo lamentamos mucho, pero no somos capaces de procesarlo." + System.lineSeparator()
                        + "Envia uno de los siguientes comandos:" + System.lineSeparator() + System.lineSeparator()
                        + "/horario - para recuperar el horario del dia" + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()
                        + "Este bot se encuentra en https://github.com/dicastro/basicfitbot");
    }

    private void handleIdealistaCommand(Update update, Command command) throws MalformedURLException, IOException {
        switch (command.getCommand()) {
        case HORARIO:
            byte[] screenshot = basicFitRest.getBasicFitTimetable();

            Path tmpDir = Paths.get("target", "tmp", "img");

            if (!tmpDir.toFile().exists()) {
                tmpDir.toFile().mkdirs();
            }
            
            Path createTempFile = Files.createTempFile(tmpDir, "horario", ".png", new FileAttribute[0]);
            File file = createTempFile.toFile();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.ENGLISH);
            
            LocalDate today = LocalDate.now();
            
            methods.sendPhoto(update.getMessage().getChat().getId(), screenshot, file, "Horario de " + today.format(formatter));

            file.delete();
            
            break;
        default:
            LOG.debug("Command received {} with arguments {}", command.getCommand(), command.getArgs());
        }
    }
}