package com.example.hellinproject.dto

import com.example.hellinproject.R
import java.io.Serializable

data class UserDTO(var uid : String? = null,                // 고유 id
                   var userProfile : String? = "https://firebasestorage.googleapis.com/v0/b/hellinproject.appspot.com/o/default_profile.png?alt=media&token=18551a8a-0711-4417-bcae-e79e270ebc25",        // 프로필 사진
                   var nickname : String? = null,           // 닉네임
                   var gender : String? = null,            // 성별
                   var birth : Int? = 0,                   // 출생년도
                   var height : Int? = 0,                  // 키
                   var weight : Int? = 0,                  // 몸무게
                   var timestamp : Long? = null            // 시간
) {
    data class Exercise(var squatCount : Int? = 0,              // 스쿼트 횟수
                        var squatTime : Int? = 0,
                        var squatAcc : Float? = 0f,
                        var pushupCount : Int? = 0,             // 푸쉬업 횟수
                        var pushupTime : Int? = 0,
                        var pushupAcc : Float? = 0f,
                        var plankTime : Int? = 0,               // 플랭크 시간
                        var plankAcc : Float? = 0f,
                        var timestamp : Long? = null            // 시간
    )
}
//    : Serializable