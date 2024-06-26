package hobbiedo.crew.kafka.type;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EntryExitType {
	ENTRY("님이 들어왔습니다."),
	EXIT("님이 나갔습니다."),
	FORCE_EXIT("님이 내보내졌습니다.");

	private final String message;
}
