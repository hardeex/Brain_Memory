package com.hardextech.brainmemory.models

class BrainGame(private val boardSize: BoardSize, customImages: List<String>?){


    val cards:List<BrainCard> = if (customImages == null){
        val chosenImage = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImage+chosenImage).shuffled()
        randomizedImages.map { BrainCard(it) }
    } else{
        val randomizedImage =(customImages + customImages).shuffled()
        randomizedImage.map { BrainCard(it.hashCode(),it) }   // use hasCode to turn imageUrl to a unique integer
    }
    var numPairsFound: Int = 0

    private var indexOfSelectedCard: Int? = null
    private var numCardFlip=0

    fun flipCard(position: Int): Boolean {
        numCardFlip++
        // This method contains the game logic
        val card = cards[position]
        /*
         flip the card after two of the cards are faced up, involves three cases:
         case 1: 0 card was previously flipped over---- flip over selected card because match is not feasible in this case
         case 2: A card previously flipped over ---- flip over the card and then check if there is a match, in order to determine
         if there is need to flip over another card or not
         case 3: 2 cards was previously flipped over---- restore the cards and let them face up again and then flip over selected card

         There can not be a case(s) where three cards are flipped over
         */
       var foundMatch = false
        if (indexOfSelectedCard==null){
            // In the case where there is either o card flipped over or two card flipped over
            restoreCard()
            indexOfSelectedCard = position
        } else{
            // in the case where exactly a card was previously flipped over
          foundMatch = checkForMatched(indexOfSelectedCard!!, position)
            // after the checkForMatch method, there will no longer be exactly one card flipped over... therefore,
            indexOfSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatched(position1: Int, position2: Int): Boolean {
        // The objective of this function is to return where those position on board are identical or
    // not---- it is going to return whether a match is found or not
        if (cards[position1].identifier != cards[position2].identifier){
            return false
        }
        // if there is a match then,
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }


    private fun restoreCard() {
        // Run through the list of all the available cards
        for (card in cards){
            //when the card us not matched
            if (!card.isMatched){
                card.isFaceUp = false
            }

        }
    }

    fun isWon(): Boolean {
        return numPairsFound==boardSize.getNumPairs()

    }

    fun isCardAlreadyFaceUp(position: Int): Boolean {
        //Grab the card at that position and check if it is face up
        return  cards[position].isFaceUp

    }

    fun getNumMoves(): Int {
        return numCardFlip/2
    }
}