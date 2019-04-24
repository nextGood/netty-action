package netty;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Google proto 测试
 *
 * @author nextGood
 * @date 2019/4/24
 */
public class PersonProtoTest {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        PersonProto.Person.Builder personBuilder = PersonProto.Person.newBuilder();
        personBuilder.setName("XXX");
        personBuilder.setEmail("xxx@qq.com");
        personBuilder.setId(111);

        PersonProto.Person.PhoneNumber.Builder phoneNumberBuilder = PersonProto.Person.PhoneNumber.newBuilder();
        phoneNumberBuilder.setNumber("000");
        phoneNumberBuilder.setType(PersonProto.Person.PhoneType.HOME);

        PersonProto.Person person = personBuilder.build();

        byte[] data = person.toByteArray();

        PersonProto.Person result = PersonProto.Person.parseFrom(data);
        System.out.println(result.getEmail());
    }
}