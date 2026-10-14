package com.perenniasystems.lascito.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // Base colors
    val amoledBlack = Color(0xFF0D0D0D)
    val goldAccent = Color(0xFFC9A96E)
    val whiteText = Color(0xFFE0E0E0)
    
    val pages = listOf(
        R.string.onboarding_slide1,
        R.string.onboarding_slide2,
        R.string.onboarding_slide3
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(amoledBlack)
    ) {
        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Illustration
                val imageRes = when (page) {
                    0 -> R.drawable.onboarding_illus_sanctuary
                    1 -> R.drawable.onboarding_illus_memories
                    else -> R.drawable.onboarding_illus_eternity
                }

                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null, // decorative
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(250.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = stringResource(pages[page]),
                    color = whiteText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
            }
        }

        // "Omitir" Button - Top Right (Drawn on top of Pager to receive clicks)
        TextButton(
            onClick = onFinishOnboarding,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.btn_skip),
                color = goldAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Controls (Dots + Next/Start Button)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager Dots
            Row(
                modifier = Modifier.height(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) goldAccent else Color.DarkGray
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                            .background(color, CircleShape)
                            .align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Action Button (Next or Start)
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinishOnboarding()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goldAccent,
                    contentColor = amoledBlack
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                val buttonText = if (pagerState.currentPage == pages.lastIndex) {
                    stringResource(R.string.btn_start)
                } else {
                    stringResource(R.string.btn_next)
                }
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
