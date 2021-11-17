package api;

import core.DownloadTask;
import core.SimpleDownloadTask;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import core.util.HttpUtils;

import java.io.IOException;

@SpringBootApplication
public class App {

	public static void main(String[] args) throws IOException {
		String yukinoRaz = "https://avatars.githubusercontent.com/u/52282402?v=4";
		String googleUrl = "https://en.wikipedia.org/wiki/tom";
		String hackerReckobs = "https://cdn.discordapp.com/attachments/902111613874765825/908724409118904350/unknown.png";
		String randomVideo = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4";
		String railwayJson = "https://raw.githubusercontent.com/datameet/railways/master/schedules.json";

		DownloadTask dt = new SimpleDownloadTask(railwayJson, 4096, "/home/saketh/Random/DMDownloads/video.mp4");
		new Thread(dt).start();

		try {
			Thread.sleep(1000);
			dt.cancel();
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		new Thread(dt).start();

		SpringApplication.run(App.class, args);
	}

}
