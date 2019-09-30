package com.acgist.snail.downloader.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acgist.snail.downloader.SingleFileDownloader;
import com.acgist.snail.net.http.HTTPClient;
import com.acgist.snail.pojo.session.TaskSession;
import com.acgist.snail.pojo.wrapper.HttpHeaderWrapper;
import com.acgist.snail.system.exception.NetException;
import com.acgist.snail.utils.FileUtils;
import com.acgist.snail.utils.IoUtils;

/**
 * <p>HTTP下载器</p>
 * 
 * @author acgist
 * @since 1.0.0
 */
public class HttpDownloader extends SingleFileDownloader {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpDownloader.class);
	
	private HttpDownloader(TaskSession taskSession) {
		super(new byte[128 * 1024], taskSession);
	}

	public static final HttpDownloader newInstance(TaskSession taskSession) {
		return new HttpDownloader(taskSession);
	}
	
	@Override
	public void open() {
		buildInput();
		buildOutput();
	}
	
	@Override
	public void download() throws IOException {
		int length = 0;
		while(ok()) {
			// TODO：阻塞线程，导致暂停不能正常结束。
			length = this.input.read(this.bytes, 0, this.bytes.length);
			if(isComplete(length)) {
				this.complete = true;
				break;
			}
			this.output.write(this.bytes, 0, length);
			this.download(length);
		}
	}

	@Override
	public void release() {
		IoUtils.close(this.input);
		IoUtils.close(this.output);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>断点续传设置（Range）：</p>
	 * <pre>
	 * Range：bytes=0-499：第0-499字节范围的内容
	 * Range：bytes=500-999：第500-999字节范围的内容
	 * Range：bytes=-500：最后500字节的内容
	 * Range：bytes=500-：从第500字节开始到文件结束部分的内容
	 * Range：bytes=0-0,-1：第一个和最后一个字节的内容
	 * Range：bytes=500-600,601-999：同时指定多个范围的内容
	 * </pre>
	 */
	@Override
	protected void buildInput() {
		final var entity = this.taskSession.entity();
		// 设置已下载大小
		final long size = FileUtils.fileSize(entity.getFile());
		final var client = HTTPClient.newInstance(entity.getUrl());
		HttpResponse<InputStream> response = null;
		try {
			response = client
				.header("Range", "bytes=" + size + "-")
				.get(BodyHandlers.ofInputStream());
		} catch (NetException e) {
			fail("HTTP请求失败");
			LOGGER.error("HTTP请求异常", e);
			return;
		}
		if(HTTPClient.ok(response) || HTTPClient.partialContent(response)) {
			final var responseHeader = HttpHeaderWrapper.newInstance(response.headers());
			this.input = new BufferedInputStream(response.body());
			if(responseHeader.range()) { // 支持断点续传
				final long begin = responseHeader.beginRange();
				if(size != begin) {
					LOGGER.warn("已下载大小和开始下载位置不相等，已下载大小：{}，开始下载位置：{}，响应头：{}", size, begin, responseHeader.allHeaders());
				}
				this.taskSession.downloadSize(size);
			} else {
				this.taskSession.downloadSize(0L);
			}
		} else if(HTTPClient.requestedRangeNotSatisfiable(response)) { // 无法满足的请求范围
			if(this.taskSession.downloadSize() == entity.getSize()) {
				this.complete = true;
			} else {
				fail("无法满足文件下载范围");
			}
		} else {
			fail("HTTP请求失败（" + response.statusCode() + "）");
		}
	}

}
