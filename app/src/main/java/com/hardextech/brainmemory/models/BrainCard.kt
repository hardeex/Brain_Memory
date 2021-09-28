package com.hardextech.brainmemory.models
/*
The objective here is to list out the attributes of the BrainCard
that is, the characteristics or features of the game
 */
data class BrainCard(
    val identifier:Int,
    val imageUrl:String?= null,
    var isFaceUp: Boolean = false,
    var isMatched:Boolean = false
)