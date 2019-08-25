import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class Main extends ListenerAdapter {
    AudioReceiver receiver = null;
    AudioSender sender = null;
    AudioManager audioManager = null;


    public static void main(String[] args) throws LoginException {
        for(File f : new File("audio_out").listFiles())
            f.delete();
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(Util.getDiscKey())
                .addEventListeners(new Main());
        builder.build();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().toLowerCase().equals("!start")) {
                if (audioManager == null)
                    audioManager = event.getGuild().getAudioManager();
                receiver = new AudioReceiver();
                sender = new AudioSender(audioManager);
                if (audioManager.isConnected()) {
                    event.getChannel().sendMessage("I am already connected to a voice chat!").queue();
                    return;
                }
                if (!event.getMember().getVoiceState().inVoiceChannel()) {
                    event.getChannel().sendMessage("Please connect to a voice chat first.").queue();
                    return;
                }
                audioManager.openAudioConnection(event.getMember().getVoiceState().getChannel());
                audioManager.setReceivingHandler(receiver);
                audioManager.setSendingHandler(sender);

            } else if (event.getMessage().getContentRaw().toLowerCase().equals("!end")) {
                if (!audioManager.isConnected()) {
                    event.getChannel().sendMessage("I am not connected to a call.").queue();
                    return;
                }
                VoiceChannel vc = audioManager.getConnectedChannel();
                if (!vc.getMembers().contains(event.getMember())) {
                    event.getChannel().sendMessage("Please connect to the same channel.").queue();
                    return;
                }
                audioManager.closeAudioConnection();

//                try {
//                    Thread.sleep(20);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                receiver.writeFiles();
                File[] files = new File("audio").listFiles();
                for (File f : files) {
                    try {
                        String baseName = f.getName().substring(0, f.getName().length() - 4);
                        System.out.println("CONVERTING " + f.getName());
                        Process p = new ProcessBuilder(
                                "ffmpeg",
                                "-f", "s16be",
                                "-ar", "48k",
                                "-ac", "2",
                                "-i", "audio" + "/" + baseName + ".txt",
                                "-f", "wav",
                                "-ar", "16k",
                                "-ac", "1",
                                "audio_out" + "/" + baseName + ".wav").start();
                        p.waitFor();
                        f.delete();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    audioManager.setReceivingHandler(null);
                    audioManager.setSendingHandler(null);
                }
                sendToNLPServer(new File("audio_out/"));
            } else if (event.getMessage().getContentRaw().toLowerCase().startsWith("!test ")) {
                sendToNLPServer(new File(event.getMessage().getContentRaw().substring("!test ".length())));
            }
        } else {
            System.out.println("bot");
        }
    }

    private void sendToNLPServer(File audioFolder) {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(Util.getAzureKey(), "westus");
        File[] audioFiles = audioFolder.listFiles();
        String[] textIDs = new String[audioFiles.length];
        String[] numberIDs = new String[audioFiles.length];
        String[] textContents = new String[audioFiles.length];
        boolean[] textContentFillingDones = new boolean[audioFiles.length];
        for(int i = 0; i < textContentFillingDones.length; i++)
            textContentFillingDones[i] = false;
        // filling txtids and nbrids
        for(int i = 0; i < audioFiles.length; i++) {
            String[] names = audioFiles[i].getName().substring(0, audioFiles[i].getName().length() - 4).split("-");
            textIDs[i] = names[0];
            numberIDs[i] = names[1];
        }
        // filling textContents
        for(int i = 0; i < audioFiles.length; i++) {
            final int I = i;
            File audioFile = audioFiles[i];
            AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getPath());
            SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
            speechRecognizer.recognized.addEventListener((o, e) -> {
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech && !e.getResult().getText().isEmpty()) {
                    System.out.print("RECOGNIZED: ");
                    System.out.println(e.getResult().getText());
                    textContents[I] = e.getResult().getText();
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    System.out.println("NO MATCH");
                }
            });
            speechRecognizer.sessionStopped.addEventListener((o, e) -> {
                System.out.print("DONE: ");
                System.out.println(e);
                textContentFillingDones[I] = true;
            });
            try {
                speechRecognizer.startContinuousRecognitionAsync().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        outer:
        while(true) {
            for(boolean b : textContentFillingDones) {
                if(b == false)
                    continue outer;
            }
            break;
        }

        for(int i = 0; i < textIDs.length; i++) {
            for(int j = 0; j < numberIDs.length; j++) {
                if(i != j) {
                    String url = "https://omnical-ai.azurewebsites.net/api/analyze" +
                            "?code=dCB9FpHWVhqwFYaxpJc5xq2cuQoBU1t9O30JS2SrPZL5iN/yAYw6QQ==" +
                            "&discordID=" + URLEncoder.encode(numberIDs[j]) +
                            "&text=" + URLEncoder.encode(textContents[i]) +
                            "&from=" + URLEncoder.encode(textIDs[i]);
                    String out = Util.executeGet(url);
                    System.out.println("STT API RESULT: " + out);
                }
            }
        }
    }
}