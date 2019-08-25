import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class AudioReceiver implements AudioReceiveHandler {
    List<FakeByteArray> byteList = new ArrayList<>();
    String userId = null;

    public boolean canReceiveUser() {
        return true;
    }

    public void handleUserAudio(UserAudio userAudio) {
        userId = Util.customID(userAudio.getUser());
        byteList.add(new FakeByteArray(userAudio.getAudioData(1.0)));

    }

    public void writeFile() {
        if (userId != null) {
            try (FileOutputStream s = new FileOutputStream(new File("audio" + "/" + userId + ".txt"))) {
                for (FakeByteArray arr : byteList)
                    s.write(arr.getArr());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
