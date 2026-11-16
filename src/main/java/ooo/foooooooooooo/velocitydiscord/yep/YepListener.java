package ooo.foooooooooooo.velocitydiscord.yep;

import cc.unilock.yeplib.api.event.YepAdvancementEvent;
import cc.unilock.yeplib.api.event.YepDeathEvent;
import cc.unilock.yeplib.api.event.YepMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class YepListener {
  private static final String DISCORD_MESSAGE_TYPE_NAME = "discord";

  private final Config config;

  public YepListener(Logger logger, Config config) {
    this.config = config;
    logger.info("YepListener created");
  }

  @Subscribe
  public void onYepAdvancement(YepAdvancementEvent event) {
    if (config.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;
    VelocityDiscord.getDiscord().onPlayerAdvancement(event.getUsername(), event.getDisplayName(), event.getTitle(), event.getDescription());
  }

  @Subscribe
  public void onYepDeath(YepDeathEvent event) {
    if (config.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;
    VelocityDiscord.getDiscord().onPlayerDeath(event.getUsername(), event.getDisplayName(), event.getMessage());
  }

  @Subscribe
  public void onYepMessage(YepMessageEvent event) {
    if (!event.getType().getName().equals(DISCORD_MESSAGE_TYPE_NAME)) return;

    var source = event.getSource();
    var server = source.getServer().getServerInfo().getName();
    if (config.serverDisabled(server)) return;

    onDiscordMessage(event.getParameters(), server);
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

    VelocityDiscord.getDiscord().onYepDiscordMessage(username, uuid, prefix, server, text, imageUrl, embedJson);
  }

  private static String param(List<String> params, int index) {
    return index < params.size() ? params.get(index) : "";
  }
}