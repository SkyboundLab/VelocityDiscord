package ooo.foooooooooooo.velocitydiscord.yep;

import cc.unilock.yeplib.api.event.YepMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class YepListener {
  private static final String YEP_NAMESPACE = "yep";
  private static final String DISCORD_MESSAGE_TYPE_NAME = "discord";
  private static final String ATTACHMENT_MESSAGE_TYPE_NAME = "attachment";
  private static final String DEATH_MESSAGE_TYPE_NAME = "death";
  private static final String ADVANCEMENT_MESSAGE_TYPE_NAME = "advancement";

  private static final Pattern ATTACHMENT_URL_PATTERN = Pattern.compile("attachment://([A-Za-z0-9._-]+)");

  private final Config config;
  private final YepAttachmentCache attachments;

  public YepListener(Logger logger, Config config) {
    this.config = config;
    this.attachments = new YepAttachmentCache(logger);
    logger.info("YepListener created");
  }

  @Subscribe
  public void onYepMessage(YepMessageEvent event) {
    var type = event.getType();
    var typeName = type.getName();
    var params = event.getParameters();

    var server = event.getSource().getServer().getServerInfo().getName();
    if (config.serverDisabled(server)) return;

    switch (typeName) {
      case DEATH_MESSAGE_TYPE_NAME -> {
        if (!type.getNamespace().equals(YEP_NAMESPACE)) return;
        VelocityDiscord.getDiscord().onPlayerDeath(param(params, 0), param(params, 1), param(params, 2));
      }

      case ADVANCEMENT_MESSAGE_TYPE_NAME -> {
        if (!type.getNamespace().equals(YEP_NAMESPACE)) return;
        VelocityDiscord.getDiscord().onPlayerAdvancement(param(params, 0), param(params, 1), param(params, 3), param(params, 4));
      }

      case ATTACHMENT_MESSAGE_TYPE_NAME -> onAttachmentMessage(params);

      case DISCORD_MESSAGE_TYPE_NAME -> onDiscordMessage(params, server);

      default -> {}
    }
  }

  private void onAttachmentMessage(List<String> params) {
    try {
      attachments.addChunk(
        param(params, 0),
        param(params, 1),
        Integer.parseInt(param(params, 2)),
        Integer.parseInt(param(params, 3)),
        param(params, 4)
      );
    } catch (NumberFormatException ignored) {}
  }

  private void onDiscordMessage(List<String> params, String server) {
    var username = param(params, 0);
    var uuid = param(params, 1);
    var text = param(params, 2);
    var imageUrl = param(params, 3);
    var embedJson = params.size() > 4 ? String.join("␟", params.subList(4, params.size())) : "";

    var prefix = Optional.<String>empty();
    if (!uuid.isEmpty()) {
      try {
        var listener = VelocityDiscord.getListener();
        if (listener != null) prefix = listener.getPrefix(UUID.fromString(uuid));
      } catch (IllegalArgumentException ignored) {}
    }

    var attachmentNames = new LinkedHashSet<String>();
    var matcher = ATTACHMENT_URL_PATTERN.matcher(embedJson);
    while (matcher.find()) attachmentNames.add(matcher.group(1));

    VelocityDiscord.getDiscord().onYepDiscordMessage(username, uuid, prefix, server, text, imageUrl, embedJson, attachments.take(attachmentNames));
  }

  private static String param(List<String> params, int index) {
    return index < params.size() ? params.get(index) : "";
  }
}