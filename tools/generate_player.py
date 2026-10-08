import os, trimesh

OUT="app/src/main/assets/footnombo_reference_player.glb"
os.makedirs(os.path.dirname(OUT), exist_ok=True)
scene=trimesh.Scene()

def material(name, rgb, rough=.55):
    return trimesh.visual.material.PBRMaterial(
        name=name, baseColorFactor=tuple(rgb)+(255,), metallicFactor=0.0, roughnessFactor=rough)

skin=material("Skin",(196,125,78)); skin2=material("SkinAccent",(174,103,62))
white=material("KitWhite",(235,238,240)); black=material("KitBlack",(25,29,34))
hair=material("Hair",(54,32,20),.8); hair2=material("HairAccent",(82,48,28),.8)
sock=material("Socks",(238,240,242)); shoe=material("Boots",(235,235,230),.35)
sole=material("BootSole",(30,32,34)); red=material("LogoRed",(180,25,25))
eye=material("Eye",(30,20,15),.35); mouth=material("Mouth",(65,18,15),.7)

def add(m,n,mat):
    m.visual.material=mat; scene.add_geometry(m,node_name=n)

def uv(n,r,c,mat,sc=(1,1,1),seg=20,rings=12):
    m=trimesh.creation.uv_sphere(radius=r,count=[seg,rings]); m.apply_scale(sc); m.apply_translation(c); add(m,n,mat)

def cyl(n,r,h,c,mat,sc=(1,1,1),sec=20):
    m=trimesh.creation.cylinder(radius=r,height=h,sections=sec); m.apply_scale(sc); m.apply_translation(c); add(m,n,mat)

def box(n,e,c,mat):
    m=trimesh.creation.box(extents=e); m.apply_translation(c); add(m,n,mat)

for side,x in [("L",-.18),("R",.18)]:
    box("Boot_"+side,(.32,.16,.62),(x,.12,.10),shoe); box("BootSole_"+side,(.34,.055,.64),(x,.045,.10),sole)
    cyl("Sock_"+side,.145,.62,(x,.48,0),sock); cyl("LowerLeg_"+side,.135,.40,(x,.98,0),skin,sc=(1,1.05,1))
box("Shorts",(1.0,.55,.56),(0,1.55,0),white)
for side,x in [("L",-.25),("R",.25)]: box("ShortLeg_"+side,(.42,.42,.52),(x,1.58,0),white); cyl("Thigh_"+side,.18,.48,(x,1.85,0),skin,sc=(1.05,1,1))

uv("Torso",.50,(0,2.34,0),white,sc=(1,.95,.62)); cyl("Collar",.22,.08,(0,2.81,0),black)
for side,x in [("L",-.47),("R",.47)]:
    uv("Shoulder_"+side,.20,(x,2.63,0),white,sc=(1.15,.95,1)); cyl("Arm_"+side,.145,.78,(x*1.04,2.22,0),skin,sc=(1,1,.9)); uv("Hand_"+side,.145,(x*1.04,1.80,0),skin,sc=(.9,1.15,.8))
for x in [-.22,.22]: box("JerseyStripe_"+str(x),(.035,.55,.02),(x,2.35,-.29),black)
box("Crest",(.16,.16,.025),(-.20,2.55,-.305),red)
box("Number1",(.055,.24,.025),(.10,2.36,-.31),black); box("Number0",(.13,.24,.025),(.25,2.36,-.31),black)

cyl("Neck",.18,.20,(0,2.91,0),skin); uv("Head",.36,(0,3.28,0),skin,sc=(.90,1.05,.84),seg=28,rings=18)
uv("EarL",.075,(-.335,3.30,0),skin,sc=(.75,1.1,.75)); uv("EarR",.075,(.335,3.30,0),skin,sc=(.75,1.1,.75))
uv("Nose",.045,(0,3.30,-.355),skin2,sc=(.75,1,.75))
for x in [-.125,.125]:
    uv("EyeWhite_"+str(x),.065,(x,3.38,-.337),white,sc=(1.2,.75,.45)); uv("Pupil_"+str(x),.032,(x,3.38,-.382),eye,sc=(1,.9,.5))
    box("Brow_"+str(x),(.13,.035,.025),(x,3.47,-.36),hair)
box("Mouth",(.15,.025,.018),(0,3.14,-.355),mouth)
for x in [-.22,.22]: uv("FaceShadow_"+str(x),.13,(x,3.15,-.28),skin2,sc=(1,.55,.3))

uv("Hair_SimpleParted",.39,(0,3.54,0),hair,sc=(.96,.50,.90),seg=28,rings=14)
for i in range(5): uv("Hair_SimpleParted_Front"+str(i),.085,(-.20+i*.10,3.60,-.27),hair2,sc=(1.2,.8,.65))
uv("Hair_Buzzed",.375,(0,3.53,0),hair,sc=(.97,.43,.90),seg=24,rings=12)
uv("Hair_Long",.40,(0,3.52,0),hair,sc=(1,.70,.94),seg=26,rings=14)
for x in [-.28,-.20,.20,.28]: cyl("Hair_Long_Lock_"+str(x),.07,.42,(x,3.25,0),hair,sc=(1,1,.8),sec=16)
uv("Hair_Buns",.39,(0,3.52,0),hair,sc=(.98,.55,.92),seg=26,rings=14)
uv("Hair_BunL",.13,(-.30,3.62,.02),hair2); uv("Hair_BunR",.13,(.30,3.62,.02),hair2)

scene.metadata["description"]="Stylized 3D football player based on the supplied reference."
scene.export(OUT,file_type="glb")
print(OUT,os.path.getsize(OUT))
