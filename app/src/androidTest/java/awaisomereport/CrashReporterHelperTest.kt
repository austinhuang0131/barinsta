package awaisomereport

import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CrashReporterHelperTest {

    @Test
    fun getErrorContent() {
        val errorContent = CrashReporterHelper.getReportContent(Exception())
        print(errorContent)
    }
}