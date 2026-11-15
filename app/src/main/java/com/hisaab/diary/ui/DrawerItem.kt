package com.hisaab.diary.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.ui.theme.HisaabTheme

/** Items in the side drawer on Home. */
enum class DrawerItem(@param:DrawableRes val icon: Int, @param:StringRes val label: Int) {
    HOME(R.drawable.ic_home, R.string.drawer_home),
    RATE(R.drawable.ic_star, R.string.drawer_rate),
    PRIVACY(R.drawable.ic_privacy, R.string.drawer_privacy),
    EXIT(R.drawable.ic_exit, R.string.drawer_exit),
}

@Composable
fun AppDrawerContent(onItemClick: (DrawerItem) -> Unit, modifier: Modifier = Modifier) {
    ModalDrawerSheet(modifier) {
        DrawerHeader()
        HorizontalDivider(Modifier.padding(bottom = 8.dp))
        DrawerItem.entries.forEach { item ->
            if (item == DrawerItem.EXIT) HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
            NavigationDrawerItem(
                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                label = { Text(stringResource(item.label)) },
                selected = item == DrawerItem.HOME,
                onClick = { onItemClick(item) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

@Composable
private fun DrawerHeader(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.drawer_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun AppDrawerPreview() {
    HisaabTheme(darkTheme = false, dynamicColor = false) {
        AppDrawerContent(onItemClick = {})
    }
}
