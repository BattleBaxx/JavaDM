package api.download;

import com.fasterxml.jackson.annotation.JsonView;

public class DownloadDTO {
	
	@JsonView(View.Get.class)
	private String url;
	@JsonView(View.Get.class)
	private String id;
	@JsonView(View.Get.class)
	private long downloadedLength;
	@JsonView(View.Get.class)
	private long totalLength;
	
	private boolean isPaused;
	
	public DownloadDTO() {
		this.url = "";
		this.id = "";
		this.downloadedLength = 0;
		this.totalLength = 0;
		this.isPaused = false;
	}
	
	public DownloadDTO(String url, String id, long downloadedLength, long totalLength) {
		this.url = url;
		this.id = id;
		this.downloadedLength = downloadedLength;
		this.totalLength = totalLength;
		this.isPaused = false;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getDownloadedLength() {
		return downloadedLength;
	}
	public void setDownloadedLength(long downloadedLength) {
		this.downloadedLength = downloadedLength;
	}
	public long getTotalLength() {
		return totalLength;
	}
	public void setTotalLength(long totalLength) {
		this.totalLength = totalLength;
	}
	
}
