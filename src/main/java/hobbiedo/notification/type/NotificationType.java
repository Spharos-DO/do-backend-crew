package hobbiedo.notification.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
	ADD_JOIN_FORM("새로운 가입 요청이 있습니다"),
	ACCEPT_JOIN_FORM("가입 요청이 수락되었습니다"),
	REJECT_JOIN_FORM("가입 요청이 거절되었습니다"),
	FORCED_EXIT_CREW("요청에 의해 내보내졌습니다");

	private final String content;
}
