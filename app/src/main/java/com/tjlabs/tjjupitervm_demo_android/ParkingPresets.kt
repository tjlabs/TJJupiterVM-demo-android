package com.tjlabs.tjjupitervm_demo_android

import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMModel.ParkingLocationState

/**
 * 섹터별 파킹 테스트용 사전 입력 데이터.
 *
 * host app 이 VM SDK 에 넘기는 parking id 는 외부 업체 시스템의 `matchingId` 이다
 * (2026-08-28 bundle 의 parking_matches). SDK 가 내부에서 GeoJSON id 로 변환한다.
 *
 * 신규 sector 에서 테스트하려면 여기 [bySectorId] 에 항목을 추가하면 된다.
 * MainActivity 는 selectedSectorId 로 조회해 초기 상태와 업데이트 상태를 넣는다.
 * 해당 sector 항목이 없으면 파킹 관련 SDK 호출은 skip 된다 (Toast 로 안내).
 */
object ParkingPresets {

    data class Entry(
        val levelId: Int,
        /** SDK init 완료 직후 setParkingLocationStates 로 전달할 초기 상태. */
        val initialStates: Map<String, ParkingLocationState> = emptyMap(),
        /** SDK init 완료 직후 setSavedParkingLocations 로 전달할 저장 목록. */
        val savedMatchingIds: List<String> = emptyList(),
        /** '점유 주차면 업데이트' 버튼 클릭 시 updateParkingLocationStates 로 전달할 상태. */
        val updateStates: Map<String, ParkingLocationState> = emptyMap(),
    )

    /**
     * key = SectorOption.id, value = 해당 sector 에서 사용할 프리셋.
     *
     * 아래 항목은 예시(기존 하드코딩 값)이며, sector id 는 실제 테스트할 sector 로 교체해서 사용한다.
     * 새 sector 에서 테스트하려면 이 map 에 항목만 추가하면 된다.
     */
    private val bySectorId: Map<Int, Entry> = mapOf(
        111 to Entry(
            levelId = 128,
            initialStates = mapOf(
                "642491532" to ParkingLocationState.OCCUPIED,
            ),
            savedMatchingIds = listOf("1725783532"),
            updateStates = mapOf(
                "765953514" to ParkingLocationState.OCCUPIED,
                "214074360" to ParkingLocationState.OCCUPIED,
                "1884931211" to ParkingLocationState.OCCUPIED,
            ),
        ),
        112 to Entry(
            levelId = 134,
            initialStates = mapOf(
                "1102" to ParkingLocationState.OCCUPIED,
            ),
            savedMatchingIds = listOf("1136"),
            updateStates = mapOf(
                "1176" to ParkingLocationState.OCCUPIED,
                "1018" to ParkingLocationState.OCCUPIED,
                "1260" to ParkingLocationState.OCCUPIED,
            ),
        ),
    )

    fun forSector(sectorId: Int?): Entry? = sectorId?.let { bySectorId[it] }
}
