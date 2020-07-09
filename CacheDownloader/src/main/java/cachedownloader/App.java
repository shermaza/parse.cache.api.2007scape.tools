package cachedownloader;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.amazonaws.regions.Regions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.runelite.cache.client.CacheClient;
import net.runelite.cache.fs.Store;
import net.runelite.protocol.api.login.HandshakeResponseType;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

	public Object handleRequest(final Object input, final Context context) {

		boolean succeeded = new File("/tmp/cache").mkdir();

		File placeholderFile = new File("/tmp/cache/main_file_cache.dat2");
		try {
			placeholderFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (Store store = new Store(new File("/tmp/cache")))
        {
        	System.out.println("Downloading and splitting cache");
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

		System.out.println("Zipping cache files.");

		try {
			new ZipFile("/tmp/cache.zip").addFolder(new File("/tmp/cache"), new ZipParameters());
		} catch (ZipException e) {
			e.printStackTrace();
		}

		System.out.println("Uploading output.");

		Regions clientRegion = Regions.DEFAULT_REGION;
		String bucketName = "cache.api.2007scape.tools";
		String key = "cache.zip";

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withRegion(clientRegion)
				.build();

		PutObjectRequest request = new PutObjectRequest(bucketName, key, new File("/tmp/cache.zip"));
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("plain/text");
		metadata.addUserMetadata("title", "cache.zip");
		request.setMetadata(metadata);
		s3Client.putObject(request);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");
		String output = String.format("{ \"message\": \"Success\"}");
		return new GatewayResponse(output, headers, 200);
	}

	private String getPageContents(String address) throws IOException {
		URL url = new URL(address);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}
}
