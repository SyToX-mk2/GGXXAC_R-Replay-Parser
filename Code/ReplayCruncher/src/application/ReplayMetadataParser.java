package application;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class ReplayMetadataParser {
    private ReplayMetadataParser() {}

    public static ReplayMetadata parse(String path) throws Exception {
        try (var channel = FileChannel.open(Path.of(path))) {
            ByteBuffer buf = ByteBuffer.allocate(136);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            channel.read(buf, 0);
            buf.flip();

            ReplayMetadata r = new ReplayMetadata();
            r.filename = Path.of(path).getFileName().toString();

            byte[] sig = new byte[12];
            buf.get(sig);
            r.signature = new String(sig, StandardCharsets.UTF_8);

            r.metadataSize = Short.toUnsignedInt(buf.getShort(0x0C));
            int metadataEnd = 0x1A + r.metadataSize;

            r.year = Short.toUnsignedInt(buf.getShort(0x1A));
            r.month = Byte.toUnsignedInt(buf.get(0x1C));
            r.day = Byte.toUnsignedInt(buf.get(0x1D));
            r.hour = Byte.toUnsignedInt(buf.get(0x1E));
            r.minute = Byte.toUnsignedInt(buf.get(0x1F));
            r.second = Byte.toUnsignedInt(buf.get(0x20));

            if (0x22 + 8 <= metadataEnd)
                r.p1SteamID = buf.getLong(0x22);
            if (0x2A + 8 <= metadataEnd)
                r.p2SteamID = buf.getLong(0x2A);
            if (0x32 + 32 <= metadataEnd)
                r.p1Name = BinaryStringUtils.readFixedString(buf, 0x32, 32);
            if (0x52 + 32 <= metadataEnd)
                r.p2Name = BinaryStringUtils.readFixedString(buf, 0x52, 32);

            r.p1Character = Byte.toUnsignedInt(buf.get(0x72));
            r.p2Character = Byte.toUnsignedInt(buf.get(0x73));
            r.optionsModified = Byte.toUnsignedInt(buf.get(0x74));
            r.matchType = Byte.toUnsignedInt(buf.get(0x75));
            r.gameVersion = Byte.toUnsignedInt(buf.get(0x76));
            r.timezoneBias = buf.getInt(0x77);
            r.p1RoundsWon = Byte.toUnsignedInt(buf.get(0x7B));
            r.p2RoundsWon = Byte.toUnsignedInt(buf.get(0x7C));
            r.matchFlags = Byte.toUnsignedInt(buf.get(0x7D));
            r.ping = Byte.toUnsignedInt(buf.get(0x7E));
            r.durationFrames = Integer.toUnsignedLong(buf.getInt(0x7F));
            r.p1Score = Byte.toUnsignedInt(buf.get(0x83));
            r.p2Score = Byte.toUnsignedInt(buf.get(0x84));
            r.p1Rank = Byte.toUnsignedInt(buf.get(0x85));
            r.p2Rank = Byte.toUnsignedInt(buf.get(0x86));
            r.winnerSide = Byte.toUnsignedInt(buf.get(0x87));
            r.isOutdated = (r.winnerSide != 1 && r.winnerSide != 2);

            return r;
        }
    }

    public static String getGameVersion(int version) {
        return version == 0 ? "R" : "AC";
    }

    public static String getMatchType(int type) {
        return type == 1 ? "Single" : "Team";
    }

    public static String getMatchFlag(int flag) {
        if (flag == 0) return null;
        if (flag == 1) return "Unfinished";
        if (flag == 2 || flag == 3) return "Disconnect";
        if (flag == 4 || flag == 5) return "Desync";
        return null;
    }
}