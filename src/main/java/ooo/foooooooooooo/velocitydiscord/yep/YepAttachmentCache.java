package ooo.foooooooooooo.velocitydiscord.yep;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

final class YepAttachmentCache {
  private static final long TTL_MILLIS = 60_000;

  private final Logger logger;
  private final Map<String, Transfer> transfers = new ConcurrentHashMap<>();
  private final Map<String, CachedFile> files = new ConcurrentHashMap<>();

  YepAttachmentCache(Logger logger) {
    this.logger = logger;
  }

  void addChunk(String transferId, String fileName, int index, int total, String chunk) {
    if (transferId.isEmpty() || fileName.isEmpty() || index < 0 || index >= total) return;

    var key = transferId + '\0' + fileName + '\0' + total;
    var transfer = transfers.computeIfAbsent(key, id -> new Transfer(total));

    transfer.chunks.put(index, chunk);
    if (transfer.chunks.size() < total) return;

    transfers.remove(key);
    try {
      var bytes = Base64.getDecoder().decode(transfer.encoded());
      files.put(fileName, new CachedFile(bytes, System.currentTimeMillis()));
    } catch (IllegalArgumentException e) {
      logger.warning("Failed to decode yep attachment " + fileName + ": " + e.getMessage());
    }
  }

  Map<String, byte[]> take(Collection<String> names) {
    var now = System.currentTimeMillis();

    files.entrySet().removeIf(entry -> now - entry.getValue().timestamp() > TTL_MILLIS);
    transfers.entrySet().removeIf(entry -> now - entry.getValue().created > TTL_MILLIS);

    var result = new HashMap<String, byte[]>();

    for (var name : names) {
      var file = files.remove(name);
      if (file != null) result.put(name, file.bytes());
    }

    return result;
  }

  private static final class Transfer {
    private final int total;
    private final long created = System.currentTimeMillis();
    private final Map<Integer, String> chunks = new ConcurrentHashMap<>();

    private Transfer(int total) {
      this.total = total;
    }

    private String encoded() {
      var encoded = new StringBuilder();

      for (int i = 0; i < total; i++) encoded.append(chunks.get(i));

      return encoded.toString();
    }
  }

  private record CachedFile(byte[] bytes, long timestamp) {
  }
}