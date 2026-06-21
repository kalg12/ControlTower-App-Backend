package com.controltower.app.email.application;

import com.controltower.app.email.domain.EmailMailboxConfig;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImapFetcherService {

    private final AesEncryptor aesEncryptor;

    /**
     * Connects to the IMAP server, fetches all UNSEEN messages from the configured folder,
     * marks them as SEEN to prevent reprocessing, then closes the connection.
     */
    public List<MimeMessage> fetchUnseen(EmailMailboxConfig config) throws MessagingException {
        String password = aesEncryptor.decrypt(config.getImapPassword());

        Properties props = buildImapProperties(config);
        Session session = Session.getInstance(props);

        Store store = session.getStore(config.isImapSsl() ? "imaps" : "imap");
        store.connect(config.getImapHost(), config.getImapPort(), config.getImapUsername(), password);

        Folder folder = store.getFolder(config.getImapFolder());
        folder.open(Folder.READ_WRITE);

        Message[] messages = folder.search(new jakarta.mail.search.FlagTerm(
            new Flags(Flags.Flag.SEEN), false
        ));

        List<MimeMessage> result = new ArrayList<>();
        for (Message msg : messages) {
            try {
                // Mark as SEEN before processing — prevents duplicate processing on failure
                msg.setFlag(Flags.Flag.SEEN, true);
                if (msg instanceof MimeMessage mimeMessage) {
                    // Load content into memory before closing connection
                    mimeMessage.getContent();
                    result.add(mimeMessage);
                }
            } catch (Exception e) {
                log.warn("Skipping malformed message in mailbox {}: {}", config.getImapUsername(), e.getMessage());
            }
        }

        folder.close(false);
        store.close();

        log.debug("Fetched {} unseen messages from {}", result.size(), config.getImapUsername());
        return result;
    }

    private Properties buildImapProperties(EmailMailboxConfig config) {
        Properties props = new Properties();
        String protocol = config.isImapSsl() ? "imaps" : "imap";
        props.put("mail.store.protocol", protocol);
        props.put("mail." + protocol + ".host", config.getImapHost());
        props.put("mail." + protocol + ".port", String.valueOf(config.getImapPort()));
        props.put("mail." + protocol + ".ssl.enable", String.valueOf(config.isImapSsl()));
        props.put("mail." + protocol + ".connectiontimeout", "10000");
        props.put("mail." + protocol + ".timeout", "10000");
        return props;
    }
}
