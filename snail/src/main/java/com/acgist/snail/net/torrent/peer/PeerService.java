package com.acgist.snail.net.torrent.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acgist.snail.config.PeerConfig;
import com.acgist.snail.config.SystemConfig;
import com.acgist.snail.utils.ArrayUtils;
import com.acgist.snail.utils.PeerUtils;

/**
 * <p>Peer Service</p>
 * <p>管理客户端的PeerId</p>
 * 
 * @author acgist
 */
public final class PeerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerService.class);
	
	private static final PeerService INSTANCE = new PeerService();
	
	public static final PeerService getInstance() {
		return INSTANCE;
	}
	
	/**
	 * <p>版本信息长度：{@value}</p>
	 */
	private static final int VERSION_LENGTH = 4;
	/**
	 * <p>PeerId前缀：{@value}</p>
	 * <p>AS=ACGIST Snail</p>
	 */
	private static final String PEER_ID_PREFIX = "AS";
	
	/**
	 * <p>PeerId</p>
	 * <p>20位系统ID</p>
	 */
	private final byte[] peerId;
	/**
	 * <p>PeerId（HTTP编码）</p>
	 */
	private final String peerIdUrl;
	
	private PeerService() {
		this.peerId = this.buildPeerId();
		this.peerIdUrl = this.buildPeerIdUrl();
		LOGGER.debug("PeerIdUrl：{}", this.peerIdUrl);
	}
	
	/**
	 * <p>生成PeerId</p>
	 * 
	 * @return PeerId
	 */
	private byte[] buildPeerId() {
		final byte[] peerIds = new byte[PeerConfig.PEER_ID_LENGTH];
		final StringBuilder builder = new StringBuilder(8);
		// 前缀：-ASXXXX-
		builder.append("-").append(PEER_ID_PREFIX);
		final String version = SystemConfig.getVersion().replace(".", "");
		if(version.length() > VERSION_LENGTH) {
			builder.append(version.substring(0, VERSION_LENGTH));
		} else {
			builder.append(version);
			builder.append("0".repeat(VERSION_LENGTH - version.length()));
		}
		builder.append("-");
		final String peerIdPrefix = builder.toString();
		final int peerIdPrefixLength = peerIdPrefix.length();
		System.arraycopy(peerIdPrefix.getBytes(), 0, peerIds, 0, peerIdPrefixLength);
		// 后缀：随机
		final int paddingLength = PeerConfig.PEER_ID_LENGTH - peerIdPrefixLength;
		final byte[] padding = ArrayUtils.random(paddingLength);
		System.arraycopy(padding, 0, peerIds, peerIdPrefixLength, paddingLength);
		return peerIds;
	}
	
	/**
	 * <p>生成PeerIdUrl</p>
	 * 
	 * @return PeerIdUrl
	 */
	private String buildPeerIdUrl() {
		return PeerUtils.urlEncode(this.peerId);
	}
	
	/**
	 * <p>获取PeerId</p>
	 * 
	 * @return PeerId
	 */
	public byte[] peerId() {
		return this.peerId;
	}
	
	/**
	 * <p>获取PeerIdUrl</p>
	 * 
	 * @return PeerIdUrl
	 */
	public String peerIdUrl() {
		return this.peerIdUrl;
	}
	
}
