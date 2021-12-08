package com.example.hellinproject.dto

data class UserDTO(var uid : String? = null,                // 고유 id
                   var userProfile : String? = null,        // 프로필 사진
                   var nickname : String? = null,           // 닉네임
                    var gender : String? = null,            // 성별
                    var birth : Int? = 0,                   // 출생년도
                    var height : Int? = 0,                  // 키
                    var weight : Int? = 0,                  // 몸무게
                    var squatCount : Int? = 0,              // 스쿼트 횟수
                    var pushupCount : Int? = 0,             // 푸쉬업 횟수
                    var plankTime : Int? = 0,               // 플랭크 시간
                    var timestamp : Long? = null)           // 시간