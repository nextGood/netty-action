package proto.struct;

/**
 * 协议栈数据结构定义
 *
 * @author nextGood
 * @date 2019/5/2
 */
public final class NettyMessage {
    /**
     * 消息头
     */
    private Header header;
    /**
     * 消息体
     */
    private Object body;

    public final Header getHeader() {
        return header;
    }

    public final void setHeader(Header header) {
        this.header = header;
    }

    public final Object getBody() {
        return body;
    }

    public final void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "NettyMessage{" + "header=" + header + ", body=" + body + '}';
    }
}