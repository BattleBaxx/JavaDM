package api;

import core.DownloadManager;
import core.DownloadTask;
import core.ParallelDownloadTask;
import core.SimpleDownloadTask;
import core.exceptions.InvalidResponseException;
import core.exceptions.InvalidUrlException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import core.util.HttpUtils;

import java.io.IOException;

@SpringBootApplication
public class App {

	public static void main(String[] args) throws IOException, InvalidUrlException {
		String yukinoRaz = "https://avatars.githubusercontent.com/u/52282402?v=4";
		String googleUrl = "https://en.wikipedia.org/wiki/tom";
		String hackerReckobs = "https://cdn.discordapp.com/attachments/902111613874765825/908724409118904350/unknown.png";
		String randomVideo = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4";
		String railwayJson = "https://raw.githubusercontent.com/datameet/railways/master/schedules.json";

		DownloadManager dm = DownloadManager.getInstance();
		dm.createDownloadTask(randomVideo, 1024, "reckobs");
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dm.pauseDownload("reckobs");
		System.out.println("Paused");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dm.resumeDownload("reckobs");
//		SpringApplication.run(App.class, args);
	}

}
