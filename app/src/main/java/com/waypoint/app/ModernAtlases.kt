package com.waypoint.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*

@Composable internal fun BoxScope.FarCryAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(Color(0xE416211A))) {AtlasTabs(s.world,a,activeColor=Color(0xFFE7E7DD));AtlasText(s.city,s.world,Modifier.padding(16.dp),24)}
    Column(Modifier.align(Alignment.CenterEnd).padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){AtlasTool(s.world,Icons.Default.LocalGasStation,"Find fuel",{a.nearby(tr("Fuel","דלק"))});AtlasTool(s.world,Icons.Default.Restaurant,"Find food",{a.nearby(tr("Food","אוכל"))});AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}
    Row(Modifier.align(Alignment.BottomCenter).padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){AtlasFind(s.world,a,Modifier.weight(1f));AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate,light=true)}
}
@Composable internal fun BoxScope.BridgesAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopStart).padding(20.dp)){AtlasText(tr("Map / INFORMATION","מפה / מידע"),s.world,size=24);AtlasText(s.city.uppercase(),s.world,size=15);FixLabel(s,Modifier.padding(top=5.dp))}
    Column(Modifier.align(Alignment.CenterStart).padding(start=20.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
    Row(Modifier.align(Alignment.BottomEnd).padding(20.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){AtlasAction(s.world,tr("SET ROUTE","קביעת מסלול"),a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate);AtlasTool(s.world,Icons.Default.ViewInAr,"Toggle 2D and 3D",a.tilt)}
}
@Composable internal fun BoxScope.SpiderAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).background(Color(0xF21C3545))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFE24350)),verticalAlignment=Alignment.CenterVertically){AtlasText(tr("MAP","מפה"),s.world,Modifier.weight(1f).padding(start=18.dp),25,color=Color.White);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}
        PlaceLabel(s,Modifier.padding(12.dp))
    }
    Column(Modifier.align(Alignment.CenterStart).padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.BookmarkBorder,"Saved",a.saved)}
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xF2183344)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){AtlasText(tr("CUSTOM WAYPOINT","נקודת ציון אישית"),s.world,Modifier.weight(1f),17);AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
}
@Composable internal fun BoxScope.StarfieldAtlas(s:NavState,b:Float,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopStart).padding(20.dp).widthIn(max=260.dp)) {AtlasText(tr("SURFACE MAP","מפת פני השטח"),s.world,size=22);HorizontalDivider(Modifier.padding(vertical=6.dp),color=Color(0xFFD3DDE3));AtlasText(s.city,s.world,size=18);FixLabel(s);Row{AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)}}
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){CompassRose(b,s.world,onRecenter=a.locate);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasAction(s.world,tr("Saved","שמורים"),a.saved);AtlasAction(s.world,tr("Locate","איתור"),a.locate)}}
}
@Composable internal fun BoxScope.SaintsAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(Color(0xF3161820)).padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){AtlasText(tr("CITY MAP","מפת העיר"),s.world,Modifier.weight(1f),28,color=Color(0xFFD3C5F5));AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)};HorizontalDivider(thickness=3.dp,color=Color(0xFF8166AE));PlaceLabel(s,Modifier.padding(top=6.dp))}
    Column(Modifier.align(Alignment.CenterStart).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){AtlasAction(s.world,tr("LOCATIONS","מיקומים"),a.search);AtlasAction(s.world,tr("SAVED","שמורים"),a.saved)}
    Row(Modifier.align(Alignment.BottomCenter).background(Color(0xEE20252E)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){AtlasFind(s.world,a,Modifier.weight(1f),tr("Search places","חיפוש מקומות"));AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}
}
@Composable internal fun BoxScope.MediciAtlas(s:NavState,a:AtlasActions) {
    AtlasFrame(s.world)
    Column(Modifier.align(Alignment.TopCenter).padding(16.dp)){AtlasTabs(s.world,a,activeColor=Color(0xFFEAD36D));AtlasText(s.city.uppercase(),s.world,Modifier.padding(top=14.dp),26)}
    Column(Modifier.align(Alignment.BottomEnd).padding(16.dp),horizontalAlignment=Alignment.End){AtlasAction(s.world,tr("PLACE WAYPOINT","הצבת נקודת ציון"),a.search);Spacer(Modifier.height(7.dp));Row(verticalAlignment=Alignment.CenterVertically){FixLabel(s);AtlasTool(s.world,Icons.Default.MyLocation,"Use my location",a.locate)}}
}
@Composable internal fun BoxScope.MafiaAtlas(s:NavState,a:AtlasActions) {
    Column(Modifier.align(Alignment.TopCenter).background(Color(0xEF23241F)).padding(horizontal=16.dp)){Row(verticalAlignment=Alignment.CenterVertically){AtlasText(tr("MAP","מפה"),s.world,Modifier.weight(1f),26,font=AtlasFont);AtlasTool(s.world,Icons.Default.Layers,"Worlds",a.huds);AtlasTool(s.world,Icons.Default.Tune,"Settings",a.settings)};HorizontalDivider(color=Color(0xFFB0A585));PlaceLabel(s)}
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xEF23241F)).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){AtlasTool(s.world,Icons.Default.Search,"Set a destination",a.search);AtlasAction(s.world,tr("Saved","שמורים"),a.saved);AtlasAction(s.world,tr("Center map","מרכוז המפה"),a.locate)}
}
