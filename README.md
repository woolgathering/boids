# sundstrom-scExtensions
Extensions to SuperCollider

## Classes
### SimpleCPU
A simple CPU meter. Needs some work... serverArg is the server we want to watch, intervalArg is the interval in seconds.

`.new(serverArg, intervalArg = 1)`

## Pseudo Ugens
### AutoBFormat_fromStereo
Creates a B-format signal from a stereo signal. Accepts a couple arguments about movement:

`.ar(in, rotate = 0, push = 0)`

### DelayS
Samplewise delay.

`.ar(in, samples = 5, mul = 1, add = 0)`

### Distance
Simulates air attenuation of higher frequencies and attenuation of volume for a sound given its distance from a subject. Radius is in meters, density is the density of the air in Pa.

`.ar(in, radius = 0, density = 0)`

### Exciter
An exciter. Need to be reworked, has a tendency to clip.

`.ar(in, cutoff = 850, gain = 3, mul = 1, add = 0)`


### TimeStretch and TimeStretchStereo
Stretches a buffer in the time domain. TimeStretch accepts a mono signal, TimeStretchStereo accepts two buffers representing the L and R channels.
#### TimeStretch
`.ar(buff, rate = 1, trans = 1, winSize = 0.2, timeDisp = 0.2, start = 0, end = 1, mul = 1, add = 0)`

#### TimeStretchStereo
If using a stereo signal, one must use Buffer.loadChannel for each channel to get into a different buffer.

`.ar(buffL, buffR, rate = 1, trans = 1, winSize = 0.2, timeDisp = 0.2, start = 0, end = 1, mul = 1, add = 0)`

## Extensions

### File
`.include(path)`

Class method. Read a file as a String and evaluate it. Hacking for dynamic includes.

### Array
`.interpolate(thisArray, thatArray, steps = 5)`

Interpolate linearly between two arrays of the same size in _steps_.
#### To Do
- Allow for interpolation along a curve; i.e. non-linear interpolation

### Date
`.yesterday`

Instance method to get a Date exactly 24 hours prior to the receiver.

`.tomorrow`

Instance method to get a Date exactly 24 hours after to the receiver.


## To Do
- Make this README cleaner...
