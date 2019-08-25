import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AudioReceiver implements AudioReceiveHandler {
    Map<String, List<FakeByteArray>> byteLists = new HashMap<>();

    public boolean canReceiveUser() {
        return true;
    }

    public void handleUserAudio(UserAudio userAudio) {
        String userId = Util.customID(userAudio.getUser());
        if(byteLists.get(userId) == null)
            byteLists.put(userId, new ArrayList<FakeByteArray>());
        byteLists.get(userId).add(new FakeByteArray(userAudio.getAudioData(1.0)));

    }

    public void writeFiles() {
        byteLists.forEach((userId, byteList) -> {
            try (FileOutputStream s = new FileOutputStream(new File("audio" + "/" + userId + ".txt"))) {
                for (FakeByteArray arr : byteList)
                    s.write(arr.getArr());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
