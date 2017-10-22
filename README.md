# sundstrom-scExtensions
Extensions to SuperCollider

## Classes
### SimpleCPU
A simple CPU meter. Needs some work...

`.new(serverArg, intervalArg = 1)`

  - __serverArg__: the server we want to track
  - __intervalArg__: the interval of time to measure the CPU in seconds

## Pseudo Ugens
### AutoBFormat_fromStereo
Creates a B-format signal from a stereo signal. Accepts a couple arguments about movement; range from 0 to 1.

`.ar(in, rotate = 0, push = 0)`

  - __in__: a stereo signal
  - __rotate__: rotate the field; positive is clockwise, negative is counterclockwise. From 0 to 1
  - __push__: push the field along the x-axis. From 0 to 1.

### DelayS
Samplewise delay.

`.ar(in, samples = 5, mul = 1, add = 0)`

  - __in__: signal in
  - __samples__: delay in samples

### Distance
Simulates air attenuation of higher frequencies and attenuation of volume for a sound given its distance from a subject.

`.ar(in, radius = 0, density = 0)`

  - __in__: signal in
  - __radius__: distance in meters from the listener
  - __density__: density of the air in Pa (not implemented)

### Exciter
An exciter. Need to be reworked, has a tendency to clip.

`.ar(in, cutoff = 850, gain = 3, mul = 1, add = 0)`

  - __in__: signal in
  - __cutoff__: the cutoff frequency of the internal high pass filter
  - __gain__: the gain to apply to the signal that is passed through the filter


### TimeStretch and TimeStretchStereo
Stretches a buffer in the time domain. TimeStretch accepts a mono signal, TimeStretchStereo accepts two buffers representing the L and R channels. Rate and transposition are independent. This pseudo-UGen loops automatically.
#### TimeStretch
`.ar(buff, rate = 1, trans = 1, winSize = 0.2, timeDisp = 0.2, start = 0, end = 1, mul = 1, add = 0)`

  - __buff__: a mono buffer
  - __rate__: the rate through which to read the file. Does not change pitch.
  - __trans__: the transposition of the sound. 1 is normal, 2 is double, 0.5 is half, etc.
  - __winSize__: the size of the window with which we read through the file. Can dramatically change the way it sounds.
  - __timeDisp__: random offset of the grains, max is winSize. Can help with phasing artifacts.
  - __start__: where to start in the file. 0 is the beginning, 1 is the end.
  - __end__: where to end in the file. 0 is the beginning, 1 is the end.

#### TimeStretchStereo
If using a stereo signal, one must use Buffer.loadChannel for each channel to get into a different buffer.

`.ar(buffL, buffR, rate = 1, trans = 1, winSize = 0.2, timeDisp = 0.2, start = 0, end = 1, mul = 1, add = 0)`

  - __buffL__: a mono buffer
  - __buffR__: a mono buffer
  - __rate__: the rate through which to read the file. Does not change pitch.
  - __trans__: the transposition of the sound. 1 is normal, 2 is double, 0.5 is half, etc.
  - __winSize__: the size of the window with which we read through the file. Can dramatically change the way it sounds.
  - __timeDisp__: random offset of the grains, max is winSize. Can help with phasing artifacts.
  - __start__: where to start in the file. 0 is the beginning, 1 is the end.
  - __end__: where to end in the file. 0 is the beginning, 1 is the end.

## Extensions

### File
`.include(path)`

Class method. Read a file as a String and evaluate it, returning the result. Hack for dynamic includes.

  - __path__: the path of the file we want to evaluate

Returns the result of the evaluation.

### Array
`.interpolate(array, steps = 5)`

Interpolate linearly between two arrays of the same size, the receiver and _array_ in _steps_.

  - __array__: an array the same size as the receiver that is the end result
  - __steps__: the number of steps in the interpolation

Returns an array of arrays including the receiver at index 0, and _array_ at index size-1.

`*.interpolate(thisArray, thatArray, steps = 5)`

Class method of the same. Interpolates between _thisArray_ and _thatArray_ in _steps_.

#### To Do
- Allow for interpolation along a curve; i.e. non-linear interpolation

### Date
`.yesterday`

Instance method to get a Date exactly 24 hours prior to the receiver.

`.tomorrow`

Instance method to get a Date exactly 24 hours after to the receiver.


## To Do
- Make this README cleaner...
