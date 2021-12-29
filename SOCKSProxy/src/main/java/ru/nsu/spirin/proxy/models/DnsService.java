package ru.nsu.spirin.proxy.models;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import ru.nsu.spirin.proxy.handlers.ConnectHandler;
import ru.nsu.spirin.proxy.handlers.Handler;
import ru.nsu.spirin.proxy.handlers.SocksRequestHandler;
import ru.nsu.spirin.proxy.socks.SocksRequest;

public final class DnsService {
    private static final Logger logger = Logger.getLogger(DnsService.class);

    private static final byte HOST_UNREACHABLE_ERROR = 0x04;
    private static final int DNS_SERVER_PORT = 53;
    private static final int BUFFER_SIZE = 1024;
    private static final int CACHE_SIZE = 256;

    private static final @Getter DnsService instance = new DnsService();

    private final Map<Integer, DnsMapValue> unresolvedNames = new HashMap<>();
    private final ResolvedNamesFiniteMap resolvedNamesCache = new ResolvedNamesFiniteMap(CACHE_SIZE);
    private final InetSocketAddress dnsServerAddress;

    private Handler dnsResponseHandler;
    private DatagramChannel datagramChannel;
    private int messageID = 0;

    private DnsService() {
        this.dnsServerAddress = new InetSocketAddress(ResolverConfig.getCurrentConfig().server(), DNS_SERVER_PORT);
    }

    public void setDatagramChannel(DatagramChannel channel) {
        this.datagramChannel = channel;
        initResponseHandler();
    }

    public void registerSelector(Selector selector) throws ClosedChannelException {
        this.datagramChannel.register(selector, SelectionKey.OP_READ, this.dnsResponseHandler);
    }

    public void resolveName(SocksRequest request, SelectionKey selectionKey) throws IOException {
        try {
            String name = request.getDomainName();
            String cachedAddress = this.resolvedNamesCache.get(name + ".");

            if (null != cachedAddress) {
                connectToTarget(cachedAddress, selectionKey, request.getTargetPort());
                return;
            }

            logger.debug("New domain name to resolve: " + request.getDomainName());
            DnsMapValue mapValue = new DnsMapValue(selectionKey, request.getTargetPort());
            Message query = getQuery(name);
            byte[] queryBytes = query.toWire();
            this.unresolvedNames.put(query.getHeader().getID(), mapValue);
            this.datagramChannel.send(ByteBuffer.wrap(queryBytes), this.dnsServerAddress);
        }
        catch (TextParseException exception) {
            SocksRequestHandler.onError(selectionKey, HOST_UNREACHABLE_ERROR);
            logger.error(exception);
        }
    }

    private void initResponseHandler() {
        this.dnsResponseHandler = new Handler(null) {
            @Override
            public void handle(SelectionKey selectionKey) throws IOException {
                ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

                if (null == datagramChannel.receive(byteBuffer)) {
                    return;
                }

                Message response = new Message(byteBuffer.flip().array());
                Record[] answers = response.getSectionArray(Section.ANSWER);
                int responseID = response.getHeader().getID();
                DnsMapValue unresolvedName = unresolvedNames.get(response.getHeader().getID());

                if (0 == answers.length) {
                    SocksRequestHandler.onError(unresolvedName.getSelectionKey(), HOST_UNREACHABLE_ERROR);
                    return;
                }

                String hostname = response.getQuestion().getName().toString();
                logger.debug(hostname + " resolved");
                String address = answers[0].rdataToString();
                resolvedNamesCache.put(hostname, address);
                connectToTarget(address, unresolvedName.getSelectionKey(), unresolvedName.getTargetPort());
                unresolvedNames.remove(responseID);
            }
        };
    }

    private void connectToTarget(String address, SelectionKey selectionKey, int port) throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        ConnectHandler.connectToTarget(selectionKey, socketAddress);
    }

    private Message getQuery(String domainName) throws TextParseException {
        Header header = new Header(this.messageID++);
        header.setFlag(Flags.RD);
        header.setOpcode(0);

        Message message = new Message();
        message.setHeader(header);

        Record record = Record.newRecord(new Name(domainName + "."), Type.A, DClass.IN);
        message.addRecord(record, Section.QUESTION);

        return message;
    }

    @RequiredArgsConstructor
    private static final class DnsMapValue {
        private final @Getter SelectionKey selectionKey;
        private final @Getter short targetPort;
    }

    @RequiredArgsConstructor
    private static final class ResolvedNamesFiniteMap {
        private final int capacity;
        private final TreeMap<String, String> map  = new TreeMap<>();

        public String get(String key){
            return this.map.get(key);
        }

        public void put(String key, String value){
            if (this.map.size() >= this.capacity) {
                this.map.remove(this.map.firstKey());
            }
            this.map.put(key, value);
        }
    }
}
