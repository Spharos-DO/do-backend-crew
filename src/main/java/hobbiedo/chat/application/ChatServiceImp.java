package hobbiedo.chat.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hobbiedo.chat.domain.Chat;
import hobbiedo.chat.domain.ChatLastStatus;
import hobbiedo.chat.dto.response.ChatDTO;
import hobbiedo.chat.dto.response.ChatHistoryDTO;
import hobbiedo.chat.dto.response.ChatImageDTO;
import hobbiedo.chat.dto.response.ChatImageListDTO;
import hobbiedo.chat.dto.response.ChatListDTO;
import hobbiedo.chat.dto.response.LastChatDTO;
import hobbiedo.chat.infrastructure.ChatLastStatusRepository;
import hobbiedo.chat.infrastructure.ChatRepository;
import hobbiedo.global.exception.GlobalException;
import hobbiedo.global.status.ErrorStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImp implements ChatService {
	private final ChatRepository chatRepository;
	private final ChatLastStatusRepository chatStatusRepository;

	@Override
	public ChatHistoryDTO getChatHistoryBefore(Long crewId, String uuid, int page) {
		int size = 10; // 페이지 당 데이터 개수
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
		Instant lastReadAt = getLastReadAt(uuid, crewId);

		List<Chat> chatList = chatRepository.findLastChatByCrewId(crewId, lastReadAt, pageable);
		if (chatList.isEmpty()) {
			throw new GlobalException(ErrorStatus.NO_EXIST_CHAT);
		}

		// 전체 페이지 수 계산 (위에서 채팅이 없을 경우 에러처리 했으니, Long 말고 long 사용)
		int totalChats = chatRepository.countByCrewIdAndCreatedAtBefore(crewId, lastReadAt);
		int lastPage = (int)Math.ceil((double)totalChats / size) - 1;

		List<ChatListDTO> chatListDtos = chatList.stream()
			.collect(Collectors.groupingBy(
				chat -> LocalDate.ofInstant(chat.getCreatedAt(), ZoneId.systemDefault())))
			.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> ChatListDTO.toDto(
				entry.getKey(),
				entry.getValue().stream()
					.sorted(Comparator.comparing(Chat::getCreatedAt))
					.map(ChatDTO::toDto)
					.toList()))
			.toList();

		return ChatHistoryDTO.toDto(lastPage, chatListDtos);
	}

	@Override
	public List<LastChatDTO> getChatList(String uuid) { // true 값을 때 0으로 보낼지 변경 후처리
		List<ChatLastStatus> crewIdList = chatStatusRepository.findByUuid(uuid);
		if (crewIdList.isEmpty()) {
			throw new GlobalException(ErrorStatus.NO_EXIST_CHAT_UNREAD_STATUS);
		}

		return crewIdList.stream()
			.map(chatStatus -> {
				Long crewId = chatStatus.getCrewId();
				// 채팅방의 마지막 메시지 조회
				Chat lastChat = chatRepository.findLastChatByCrewId(crewId)
					.orElseThrow(() -> new GlobalException(ErrorStatus.NO_EXIST_CHAT));
				String content = lastChat.getImageUrl() != null ? "사진을 보냈습니다." : lastChat.getText();
				// 안 읽은 메시지 개수 조회
				long count = chatRepository.countByCrewIdAndCreatedAtAfter(crewId,
					getLastReadAt(uuid, crewId));
				int cnt = count > 999 ? 999 : (int)count;

				return LastChatDTO.toDto(crewId, content, cnt, lastChat.getCreatedAt());
			})
			.sorted(Comparator.comparing(LastChatDTO::getCreatedAt).reversed())
			.toList();
	}

	private Instant getLastReadAt(String uuid, Long crewId) {
		return chatStatusRepository.findByUuidAndCrewId(crewId, uuid)
			.map(ChatLastStatus::getLastReadAt)
			.orElseThrow(() -> new GlobalException(ErrorStatus.NO_EXIST_CHAT_UNREAD_STATUS));
	}

	@Override
	public List<ChatImageListDTO> getChatsWithImageUrl(Long crewId) {
		List<Chat> chatList = chatRepository.findByCrewIdAndImageUrl(crewId);
		if (chatList.isEmpty()) {
			throw new GlobalException(ErrorStatus.NO_EXIST_IMAGE_CHAT);
		}

		return chatList.stream()
			.collect(Collectors.groupingBy(
				chat -> LocalDate.ofInstant(chat.getCreatedAt(), ZoneId.systemDefault())))
			.entrySet().stream()
			.sorted(Map.Entry.<LocalDate, List<Chat>>comparingByKey().reversed())
			.map(entry -> ChatImageListDTO.toDto(entry.getKey(),
				entry.getValue().stream()
					.map(ChatImageDTO::toDto)
					.sorted(Comparator.comparing(ChatImageDTO::getCreatedAt).reversed())
					.toList()))
			.toList();
	}

	@Transactional
	@Override
	public void createChatStatus(Long crewId, String uuid) {
		chatStatusRepository.save(ChatLastStatus.builder()
			.uuid(uuid)
			.crewId(crewId)
			.connectionStatus(false)
			.lastReadAt(Instant.now())
			.build());
	}

	@Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
	@Transactional
	@Override
	public void deleteOldChatsWithImageUrl() {
		// 3개월 전 시각 계산
		LocalDateTime expiryDateTime = LocalDateTime.now().minusMonths(3);

		// LocalDateTime을 Instant로 변환
		Instant expiryDate = expiryDateTime.atZone(ZoneId.systemDefault()).toInstant();
		chatRepository.deleteByImageUrlExistsAndCreatedAtBefore(expiryDate);
	}
}
