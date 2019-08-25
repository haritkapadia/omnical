import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

class AudioSender implements AudioSendHandler {
    AudioManager audioManager;

    AudioSender(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    @Override
    public boolean canProvide() {
        return audioManager.isConnected();
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return null;
    }
}