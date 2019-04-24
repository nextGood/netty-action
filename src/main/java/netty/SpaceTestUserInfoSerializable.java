package netty;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Java序列化后码流空间测试
 *
 * @author nextGood
 * @date 2019/4/23
 */
public class SpaceTestUserInfoSerializable {

    public static void main(String[] args) throws Exception {
        UserInfo info = new UserInfo();
        info.buildUserId(100).buildUserName("Welcome to Netty");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(info);
        os.flush();
        os.close();
        byte[] bytes = bos.toByteArray();
        System.out.println("The jdk serializable length is :" + bytes.length);
        bos.close();
        System.out.println("---------------------------");
        System.out.println("The byte array serializable length is : " + info.codeC().length);
    }

    private static class UserInfo implements Serializable {
        private static final long serialVersion = 1;
        private String userName;
        private int userId;

        public UserInfo buildUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public UserInfo buildUserId(int userId) {
            this.userId = userId;
            return this;
        }

        public final String getUserName() {
            return userName;
        }

        public final void setUserName(String userName) {
            this.userName = userName;
        }

        public final int getUserId() {
            return userId;
        }

        public final void setUserId(int userId) {
            this.userId = userId;
        }

        public byte[] codeC() {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            byte[] value = this.userName.getBytes();
            buffer.putInt(value.length);
            buffer.put(value);
            buffer.putInt(this.userId);
            buffer.flip();
            value = null;
            byte[] result = new byte[buffer.remaining()];
            buffer.get(result);
            return result;
        }
    }
}