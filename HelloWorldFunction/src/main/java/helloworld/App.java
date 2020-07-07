package helloworld;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import net.runelite.cache.client.CacheClient;
import net.runelite.cache.fs.Store;
import net.runelite.protocol.api.login.HandshakeResponseType;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

	public Object handleRequest(final Object input, final Context context) {

		/* Download the client.zip file from S3 */
		Regions clientRegion = Regions.DEFAULT_REGION;
		String bucketName = "cache.api.2007scape.tools";
		String key = "cache.zip";

		S3Object fullObject = null, objectPortion = null, headerOverrideObject = null;
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withRegion(clientRegion)
				.build();

		System.out.println("Downloading an object");
		fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key));

		InputStream reader = new BufferedInputStream(
				fullObject.getObjectContent());
		File file = new File("/tmp/cache.zip");
		OutputStream writer = null;

		try {
			writer = new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int read = -1;

		while (true) {
			try {
				if (!(( read = reader.read() ) != -1)) break;
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				writer.write(read);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Downloaded to /tmp/cache.zip");

//        try (Store store = new Store(new File("/tmp/cache")))
//        {
//            store.load();
//
//            CacheClient c = new CacheClient(store, 190);
//            c.connect();
//            CompletableFuture<HandshakeResponseType> handshake = c.handshake();
//
//            HandshakeResponseType result = handshake.get();
//
//            c.download();
//
//            c.close();
//
//            store.save();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		}

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
