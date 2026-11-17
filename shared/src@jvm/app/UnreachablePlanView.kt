package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.baconana.mybeautifulgit_chan.domain.UnreachablePlan
import org.baconana.mybeautifulgit_chan.i18n.Strings
import org.baconana.mybeautifulgit_chan.ui.mockup.Term

/** Общие строки плана потерь для reset и rewrite. */
@Composable
internal fun UnreachablePlanDetails(
    plan: UnreachablePlan,
    strings: Strings,
) {
    if (plan.commitCount == 0) {
        Text(
            strings.commitActionResetNoLostCommits,
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.greenDim,
        )
    } else {
        Text(
            "${plan.commitCount} ${strings.commitActionResetLostCommits}",
            fontSize = 8.5.sp,
            fontFamily = Term.mono,
            color = Term.amber,
        )
        plan.commits.take(5).forEach { commit ->
            Text(
                "  · ${commit.shortHash} ${commit.subject}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (plan.commitCount > plan.commits.size) {
            Text(
                "  +${plan.commitCount - plan.commits.size} ${strings.commitActionResetMore}",
                fontSize = 8.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
    }

    if (plan.objectCount > 0) {
        Spacer(Modifier.height(3.dp))
        Text(
            "${plan.objectCount} ${strings.commitActionUnreachableObjects}",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.dim,
        )
    }

    Spacer(Modifier.height(4.dp))
    if (plan.fsckError != null) {
        Text(
            "${strings.commitActionFsckWarning}: ${plan.fsckError}",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.red,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    } else if (plan.fsckUnreachableObjects > 0) {
        Text(
            "${plan.fsckUnreachableObjects} ${strings.commitActionFsckAlready}" +
                " · ${plan.fsckUnreachableCommits} ${strings.commitActionFsckCommits}",
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    } else {
        Text(
            strings.commitActionFsckClean,
            fontSize = 8.sp,
            fontFamily = Term.mono,
            color = Term.faint,
        )
    }
}
