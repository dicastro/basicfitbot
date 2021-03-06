package es.qopuir.basicfitbot;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.qopuir.basicfitbot.internal.CommandImpl;
import es.qopuir.telegrambot.model.Message;
import es.qopuir.telegrambot.model.Update;

@RestController
public class BotController {
    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    public static final String BASICFITBOT_URL = "/basicfitbot";

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @RequestMapping("/ping")
    public void ping() throws IOException {
        LOG.info("ping ok!");
    }

    @RequestMapping(method = RequestMethod.POST, value = BASICFITBOT_URL)
    public void idealistabotRequest(@RequestBody Update update) throws IOException {
        if (update != null && update.getMessage() != null && !StringUtils.isEmpty(update.getMessage().getText())) {
            LOG.debug("Received update -> {}", jacksonObjectMapper.writer().writeValueAsString(update));
            Command command = getCommand(update);
            commandHandler.handleCommand(update, command);
        } else {
            LOG.error("Received update without text -> {}", jacksonObjectMapper.writer().writeValueAsString(update));
        }
    }

    Command getCommand(Update update) {
        Message message = update.getMessage();

        String text = message.getText().trim();

        CommandImpl command = new CommandImpl();

        int commandIndex = text.indexOf(" ");

        if (commandIndex != -1) {
            command.setCommand(text.substring(0, commandIndex));
            command.setArgs(text.substring(commandIndex + 1));
        } else {
            command.setCommand(text);
        }

        return command;
    }

    @RequestMapping("/sampleKeyboard")
    public void sampleKeyboard() {
        //
        // UpdateResponse updates = methods.getUpdates();
        //
        // System.out.println(updates);
        //
        // String[][] buttons = new String[3][1];
        // buttons[0][0] = "Eins";
        // buttons[1][0] = "Zwei";
        // buttons[2][0] = "Drei";
        //
        // ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        // replyKeyboardMarkup.setKeyboard(buttons);
        //
        // MultiValueMap<String, Object> vars = new LinkedMultiValueMap<String,
        // Object>();
        // vars.add("chat_id", 3130440);
        // vars.add("text", "Hallo vom Spring BotController");
        // vars.add("reply_markup", buttons);

        // methods.sendMessage(3130440, "Hallo vom Spring BotController", false,
        // 0, replyKeyboardMarkup);
    }
}