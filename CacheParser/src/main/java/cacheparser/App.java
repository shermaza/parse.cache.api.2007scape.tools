package cacheparser;

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
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.runelite.cache.Cache;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

	public Object handleRequest(final Object input, final Context context) {

		System.out.println("Downloading cache files from S3");

		Regions clientRegion = Regions.DEFAULT_REGION;
		String bucketName = "cache.api.2007scape.tools";
		String key = "cache.zip";

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withRegion(clientRegion)
				.build();

		S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));

		InputStream reader = new BufferedInputStream(
				object.getObjectContent());
		File file = new File("/tmp/cache.zip");
		OutputStream writer = null;

		System.out.println("Writing cache files to /tmp/cache.zip");

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
			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


		String cacheDir = "/tmp/cache"; //n.b. unzipping cache.zip turns into /tmp/cache
		System.out.println("Unzipping cache archive to /tmp/cache.");
		try {
			ZipFile zipFile = new ZipFile("/tmp/cache.zip");
			zipFile.extractAll("/tmp");
		} catch (ZipException e) {
			e.printStackTrace();
		}

		File itemsDir = new File("/tmp/items");
		itemsDir.mkdir();

		System.out.println("Parsing items.");
		try {
			Cache.main(new String[]{"--cache", cacheDir, "--items", "/tmp/items"});
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Zipping files.");

		try {
			new ZipFile("/tmp/items.zip").addFolder(itemsDir, new ZipParameters());
		} catch (ZipException e) {
			e.printStackTrace();
		}

		System.out.println("Uploading to S3");

		PutObjectRequest request = new PutObjectRequest(bucketName, "items.zip", new File("/tmp/items.zip"));
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("plain/text");
		metadata.addUserMetadata("title", "items.zip");
		request.setMetadata(metadata);
		s3Client.putObject(request);

		// Now do the same for sprites

		File spritesDir = new File("/tmp/sprites");
		spritesDir.mkdir();

		System.out.println("Parsing sprites.");
		try {
			Cache.main(new String[]{"--cache", cacheDir, "--sprites", "/tmp/sprites"});
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Zipping files.");

		try {
			new ZipFile("/tmp/sprites.zip").addFolder(spritesDir, new ZipParameters());
		} catch (ZipException e) {
			e.printStackTrace();
		}

		System.out.println("Uploading to S3");

		request = new PutObjectRequest(bucketName, "sprites.zip", new File("/tmp/sprites.zip"));
		metadata = new ObjectMetadata();
		metadata.setContentType("plain/text");
		metadata.addUserMetadata("title", "sprites.zip");
		request.setMetadata(metadata);
		s3Client.putObject(request);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");
		String output = String.format("{ \"message\": \"Success\"}");
		return new GatewayResponse(output, headers, 200);
	}
}
