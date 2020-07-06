package helloworld;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import net.runelite.cache.client.CacheClient;
import net.runelite.cache.fs.Store;
import net.runelite.protocol.api.login.HandshakeResponseType;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

	public Object handleRequest(final Object input, final Context context) {

        try (Store store = new Store(new File("/tmp/cache")))
        {
            store.load();

            CacheClient c = new CacheClient(store, 190);
            c.connect();
            CompletableFuture<HandshakeResponseType> handshake = c.handshake();

            HandshakeResponseType result = handshake.get();

            c.download();

            c.close();

            store.save();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");
		try {
			final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
			String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);
			return new GatewayResponse(output, headers, 200);
		} catch (IOException e) {
			return new GatewayResponse("{}", headers, 500);
		}
	}

	private String getPageContents(String address) throws IOException {
		URL url = new URL(address);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}
}
