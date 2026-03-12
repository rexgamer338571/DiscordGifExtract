package dev.ng5m.dge;

import dev.ng5m.dge.protobuf.FrecencyUserSettingsPB;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscordGifExtract {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: DiscordGifExtract <2_file_path> <download | count>");
            return;
        }

        boolean download = args[1].equalsIgnoreCase("download");

        String two = Files.readString(Path.of(args[0]));
        int len = "{\"settings\":\"".length();

        String base64 = two.substring(len, two.length() - 4);

        byte[] data = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));

        FrecencyUserSettingsPB.FrecencyUserSettings fus = FrecencyUserSettingsPB.FrecencyUserSettings
                .newBuilder()
                .mergeFrom(data)
                .build();

        if (download) {
            List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>();
            List<FrecencyUserSettingsPB.FrecencyUserSettings.FavoriteGIF> gifs = fus.getFavoriteGifs().getGifsMap()
                    .values()
                    .stream().toList();

            HttpClient httpClient = HttpClient.newHttpClient();
            for (int i = 0; i < fus.getFavoriteGifs().getGifsCount(); i++) {
                FrecencyUserSettingsPB.FrecencyUserSettings.FavoriteGIF gif = gifs.get(i);

                try {
                    var request = HttpRequest.newBuilder()
                            .uri(URI.create(gif.getSrc()))
                            .build();

                    var future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());

                    future.thenAccept(response -> {
                        String[] split = gif.getSrc().split("/");
                        try {
                            Files.write(Path.of("gifs/" + split[split.length - 1]), response.body());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    futures.add(future);
                } catch (Exception x) {
                    continue;
                }
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            System.out.println("downloaded " + futures.size() + " gifs");
        } else {
            System.out.println("counted " + fus.getFavoriteGifs().getGifsCount() + " gifs");
        }
    }

}
