package tech.goksi.pterobot.util

object Common  {
    inline fun <T> checkInput(currentInput: T, defaultValue: T,message: String , expression: () -> T): Pair<T, Boolean> {
        var fixed: T
        var edited = false
        if(currentInput == null || currentInput == defaultValue){
            edited = true
            do{
                Checks.logger.info(message)
                fixed = expression.invoke()?:defaultValue
            } while (fixed == defaultValue)
        }else fixed = currentInput
        return Pair(fixed, edited)
    }
}